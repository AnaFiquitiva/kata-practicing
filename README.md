# Kata Practicing – Registro de Practicantes, Banco de Bogotá (Reto técnico)

Herramienta web para que los **practicantes** registren sus datos y hoja de vida, y para que los **analistas de selección** los revisen y los marquen como **Viable** o **No viable**.

El proyecto está separado en **front-end** y **back-end**, cada uno en su carpeta y en su propio contenedor:

| Capa | Tecnología | Carpeta |
|---|---|---|
| Front-end | HTML5 + CSS3 + JavaScript (sin frameworks, Fetch API), servido por **Nginx** | `frontend/` |
| Back-end | **Java 21** · Spring Boot 3.3 (Web, Data JPA, Validation, Security, Actuator) · Maven | `backend/` |
| Base de datos | SQLite | `backend_data/db/` |
| Hojas de vida | Archivos PDF en disco | `backend_data/uploads/` |
| Infraestructura | Docker · Docker Compose · Dockerfile multi-stage (JDK 21 → JRE 21) | raíz |

![Formulario de registro](docs/capturas/01-registro.png)

## Arquitectura

```
Navegador ──► Nginx :3000 (practicantes-ui)
               ├── /          → archivos estáticos de frontend/
               └── /api/**    → proxy ──► Spring Boot :8080 (practicantes-api)
                                            ├── SQLite  → /app/data/practicantes.db  (volumen backend_data/db)
                                            └── PDFs    → /app/uploads               (volumen backend_data/uploads)
```

Nginx actúa como *reverse proxy*: el navegador ve front y API en el **mismo origen**. Por eso no hace falta CORS, y la sesión del analista viaja en una cookie `HttpOnly` protegida con CSRF.

## Requisitos previos

- **Docker Desktop** (o Docker Engine + Compose v2).
- Solo para ejecutar sin Docker o correr pruebas: **JDK 21** y **Maven 3.9+**.

## Cómo ejecutar el proyecto

```bash
docker compose up --build -d
```

| Aplicación | URL |
|---|---|
| Formulario del practicante | http://localhost:3000 |
| Panel del analista | http://localhost:3000/analista.html |
| API (back-end directo) | http://localhost:8080/api · health: http://localhost:8080/actuator/health |

**Credenciales del analista:** usuario `analista` · contraseña `Bogota2026*`

El front-end espera a que el back-end esté *healthy* (health check de Actuator) antes de arrancar.

### Comandos útiles

```bash
docker compose down              # detener (los datos se conservan)
docker compose logs -f           # ver logs
docker compose up --build -d     # reconstruir tras cambios en el back-end
```

> No hace falta `docker compose down -v` para detener el proyecto, y no se deben borrar a mano los archivos de `backend_data/`: ahí viven la base de datos y las hojas de vida.

### Variables opcionales

Si los puertos 3000 u 8080 ya están ocupados, o quieres otras credenciales:

```powershell
$env:FRONTEND_PORT="3080"; $env:BACKEND_PORT="18080"; $env:ANALISTA_PASSWORD="OtraClave123"; docker compose up --build -d
```

| Variable | Por defecto |
|---|---|
| `FRONTEND_PORT` | `3000` |
| `BACKEND_PORT` | `8080` |
| `ANALISTA_USUARIO` | `analista` |
| `ANALISTA_PASSWORD` | `Bogota2026*` |

## Persistencia

| Dato | En el contenedor | En tu equipo |
|---|---|---|
| Base de datos SQLite | `/app/data/practicantes.db` | `backend_data/db/practicantes.db` |
| Hojas de vida (PDF) | `/app/uploads/<uuid>.pdf` | `backend_data/uploads/` |

Se usan *bind mounts*, así que la información sobrevive a `docker compose down`, a reinicios y a reconstrucciones de imagen.

## Ejecutar sin Docker (desarrollo del back-end)

```bash
cd backend
mvn spring-boot:run
```

Usa las mismas carpetas `../backend_data/db` y `../backend_data/uploads`. El front-end necesita el proxy de Nginx para hablar con la API, así que lo más simple es levantar solo ese contenedor apuntando al back-end local:

```powershell
docker run --rm -p 3000:80 -e BACKEND_URL=http://host.docker.internal:8080 -e NGINX_ENVSUBST_FILTER=^BACKEND_URL$ `
  -v "${PWD}/frontend:/usr/share/nginx/html:ro" `
  -v "${PWD}/frontend/nginx/default.conf.template:/etc/nginx/templates/default.conf.template:ro" nginx:1.27-alpine
```

## Pruebas

```bash
cd backend
mvn test
```

Hay 9 pruebas de integración (`PracticanteApiTest`) que cubren: el flujo completo (registrar → listar → marcar viable → descargar), campos obligatorios vacíos, archivo que no es PDF, correo duplicado, acceso sin autenticación, estado inválido, login correcto e incorrecto, y el health check.

Además se verificó con Docker Compose, de punta a punta a través de Nginx: registro con PDF, 401 sin sesión, login, listado, cambio de estado, descarga, logout y persistencia de datos y PDF tras `down`/`up`.

## Funcionalidades

### Practicante (público)
- Formulario con nombre completo, correo, carrera, semestre (1–12) y hoja de vida en PDF.
- Validación en el navegador (JavaScript) **y** en el servidor (Jakarta Validation): campos obligatorios, formato de correo, PDF ≤ 5 MB.
- El servidor verifica que el archivo sea realmente un PDF (firma `%PDF-`), no solo la extensión.
- No permite registrar dos veces el mismo correo.

### Analista (requiere inicio de sesión)
- Resumen con totales: registrados, pendientes, viables y no viables.
- Lista de practicantes con búsqueda por nombre, correo o carrera, y filtro por estado.
- Detalle de cada practicante con botones **Viable** / **No viable**.
- Descarga de la hoja de vida.
- Cierre de sesión.

### Desafíos opcionales
- ✅ **Autenticación básica**: Spring Security con usuario y contraseña (BCrypt), sesión en cookie HttpOnly y CSRF.
- ✅ **Descarga de archivos**: el analista descarga la hoja de vida con su nombre original.

## API REST

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/csrf` | Público | Token CSRF (`{ token, headerName }`) para `POST`/`PATCH` |
| `POST` | `/api/auth/login` | Público | Inicia sesión (`application/x-www-form-urlencoded`: `username`, `password`) → `200 {"usuario"}` o `401` |
| `POST` | `/api/auth/logout` | Analista | Cierra la sesión → `204` |
| `GET` | `/api/auth/me` | Analista | Usuario autenticado, o `401` |
| `POST` | `/api/practicantes` | Público | Registra un practicante (`multipart/form-data`: `nombreCompleto`, `correo`, `carrera`, `semestre`, `hojaVida`) |
| `GET` | `/api/practicantes?estado=VIABLE` | Analista | Lista practicantes (filtro opcional `PENDIENTE` · `VIABLE` · `NO_VIABLE`) |
| `GET` | `/api/practicantes/{id}` | Analista | Detalle de un practicante |
| `PATCH` | `/api/practicantes/{id}/estado` | Analista | Cambia el estado. Cuerpo: `{"estado": "VIABLE"}` |
| `GET` | `/api/practicantes/{id}/hoja-vida` | Analista | Descarga el PDF |
| `GET` | `/actuator/health` | Público | Estado del servicio (lo usa Docker) |

Errores en formato uniforme:

```json
{ "estado": 400, "mensaje": "Hay campos inválidos en el formulario",
  "errores": { "correo": "El correo electrónico no es válido" }, "fecha": "..." }
```

## Estructura del proyecto

```
kata-practicing/
├── docker-compose.yml
├── README.md
├── frontend/                               # Contenedor Nginx
│   ├── index.html                          # Formulario del practicante
│   ├── login.html, analista.html           # Acceso y panel del analista
│   ├── marca.html                          # Identidad visual y fuentes
│   ├── css/styles.css
│   ├── js/comun.js, registro.js, login.js, analista.js
│   ├── img/                                # Logos oficiales
│   └── nginx/default.conf.template         # Proxy /api → back-end y cabeceras de seguridad (CSP)
├── backend/                                # Contenedor Spring Boot (Java 21)
│   ├── Dockerfile                          # Multi-stage: maven:3.9.9-eclipse-temurin-21 → eclipse-temurin:21-jre-alpine
│   ├── pom.xml
│   └── src/main/java/com/bdb/katapracticing
│       ├── config/SecurityConfig.java      # Login/logout por API, CSRF, rutas protegidas
│       ├── controller/                     # PracticanteController, SesionController
│       ├── dto/                            # Request/response (no expone rutas internas)
│       ├── exception/                      # Manejo global de errores
│       ├── model/                          # Practicante, EstadoPracticante
│       ├── repository/
│       └── service/                        # PracticanteService, AlmacenamientoArchivosService
├── backend_data/                           # Volúmenes persistentes (ignorado por git)
│   ├── db/
│   └── uploads/
└── docs/capturas/
```

## Decisiones de diseño y seguridad

- **Front y back separados.** El front no contiene lógica de servidor ni credenciales: solo consume la API. La autenticación ocurre en el back-end, y el panel redirige al login si `/api/auth/me` responde `401`.
- **Mismo origen gracias al proxy de Nginx.** No hay que abrir CORS y la cookie de sesión puede ser `HttpOnly`.
- Los PDF se guardan con nombre **UUID**; el nombre original solo se usa al descargar. Así no hay colisiones ni *path traversal*.
- Si falla el guardado en la base de datos, el archivo subido se elimina, de modo que no quedan archivos huérfanos.
- El front construye el DOM con `textContent` (no `innerHTML`) para evitar XSS. Nginx envía una *Content Security Policy* estricta, `X-Frame-Options` y `X-Content-Type-Options`.
- Límite de subida de 5 MB, alineado en Spring (`multipart`) y en Nginx (`client_max_body_size`).
- Java 21 en todo el ciclo: `java.version` y `maven.compiler.release` en el `pom.xml`, JDK 21 para compilar y JRE 21 para ejecutar.

## Identidad visual y fuentes

La interfaz usa el logotipo y los colores oficiales del Banco de Bogotá. El detalle completo está en `/marca.html`.

**Logotipos:** se descargaron de la [Sala de Prensa del Banco de Bogotá – Logos y marca](https://saladeprensa.bancodebogota.com/logos-y-marca/) (archivos de diciembre de 2022) y se guardaron en `frontend/img/`. Solo se recortaron los márgenes.

**Colores verificados.** Se contaron los píxeles de los PNG oficiales:

| Uso | HEX | Evidencia |
|---|---|---|
| Azul institucional | `#14327D` | Texto del logotipo y fondo de la versión en blanco |
| Amarillo del símbolo | `#FFBE00` | Símbolo en todos los PNG oficiales |
| Rojo del símbolo | `#CD3232` | Símbolo en todos los PNG oficiales |

**Pendiente de verificar.** Una referencia secundaria atribuye al *Manual de Manejo de Marca 2022* una paleta de comunicación (naranja `#D7733C`, amarillo `#EBCD5A`, verde `#919150`, azul `#8CAFBE`, rosa `#F0BEC3` y lila `#C8AABE`) y un amarillo de logo `#EBB932` que **no** coincide con los archivos digitales oficiales (`#FFBE00`). No se pudo confirmar en el [PDF oficial del manual](https://www.bancodebogota.com/wps/themes/html/banco-de-bogota/pdf/nuestra-organizacion/manual-de-marca.pdf), así que esos colores no se usan.

**Tipografía:** la corporativa no es pública; se usa [Montserrat](https://fonts.google.com/specimen/Montserrat) (SIL OFL) como sustituto.

> "Banco de Bogotá" y su logotipo son marcas registradas del Banco de Bogotá. Aquí se usan solo con fines académicos, para un reto técnico. Esta aplicación no es un sitio oficial del banco.

## Cumplimiento del reto

| Requisito | Estado | Dónde |
|---|---|---|
| Formulario: nombre, correo, carrera, semestre | ✅ | `frontend/index.html` |
| Subir hoja de vida (PDF) | ✅ | `frontend/index.html`; el servidor valida la firma `%PDF-` |
| Validación de campos obligatorios (JS) | ✅ | `frontend/js/registro.js`, más Jakarta Validation en el servidor |
| Panel: lista, detalle, botones Viable / No viable | ✅ | `frontend/analista.html`, `frontend/js/analista.js` |
| API: guardar, listar, actualizar estado | ✅ | `PracticanteController` |
| Base de datos local SQLite | ✅ | `backend_data/db/practicantes.db` |
| Archivos en carpeta del servidor | ✅ | `backend_data/uploads/` |
| README con instalación, ejecución y capturas | ✅ | Este archivo |
| Opcional 1: autenticación básica | ✅ | `SecurityConfig` |
| Opcional 2: descarga de la hoja de vida | ✅ | `GET /api/practicantes/{id}/hoja-vida` |

## Capturas

| Registro | Login analista |
|---|---|
| ![Registro](docs/capturas/01-registro.png) | ![Login](docs/capturas/03-login.png) |

![Identidad visual y fuentes](docs/capturas/04-identidad-visual.png)

> Pendiente: captura del panel del analista en `docs/capturas/02-panel-analista.png`.
