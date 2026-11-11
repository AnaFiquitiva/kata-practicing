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

### Estilo arquitectónico: monolito modular

- **Cliente-servidor desacoplado:** el front-end es estático (HTML/CSS/JS sin frameworks) y solo habla con la API REST por JSON.
- **Back-end como monolito modular** con [Spring Modulith](https://spring.io/projects/spring-modulith): una sola aplicación y un solo despliegue, dividida por **áreas de negocio** (no por capas técnicas). Cada módulo esconde sus detalles y solo expone una API pequeña.

```
                ┌──────────────────────┐
                │     practicantes     │  registro + decisión del analista
                │  (dueño de la tabla) │
                └──────┬────────┬──────┘
          usa la API   │        │
          pública      ▼        │
       ┌────────────────────┐   │       ┌──────────────┐
       │     hojasvida      │   │       │  seguridad   │  login, sesión, CSRF
       │ validar/guardar PDF│   │       └──────┬───────┘
       └─────────┬──────────┘   │              │
                 ▼              ▼              ▼
          ┌─────────────────────────────────────────┐
          │                compartido               │  rutas, ErrorResponse,
          └─────────────────────────────────────────┘  ErrorDeNegocio
```

| Módulo | Responsabilidad | API pública (lo que otros pueden usar) | Puede depender de |
|---|---|---|---|
| `practicantes` | Registrar practicantes y decidir Viable / No viable | — (nadie lo necesita) | `compartido`, `hojasvida` |
| `hojasvida` | Validar, guardar y entregar los PDF | `HojasVidaService`, `HojaVidaGuardada`, `HojaVidaDescargable`, `ArchivoInvalidoException` | `compartido` |
| `seguridad` | Autenticación del analista y reglas de acceso | — | `compartido` |
| `compartido` | Contrato común de la API | `Rutas`, `ErrorResponse`, `ErrorDeNegocio`, `TipoError`, `RecursoNoEncontradoException` | nada |

**Reglas (se verifican en cada `mvn test`):**

1. **Lo que está en la raíz del paquete de un módulo es público; lo que está en subpaquetes es interno.** Por ejemplo, `hojasvida.internal.AlmacenamientoLocalHojasVida` no se puede usar fuera de `hojasvida`.
2. **Cada módulo declara de quién puede depender** con `@ApplicationModule(allowedDependencies = ...)` en su `package-info.java`.
3. **No hay ciclos** entre módulos.

```java
// practicantes/package-info.java
@ApplicationModule(displayName = "Practicantes", allowedDependencies = {"compartido", "hojasvida"})
package com.bdb.katapracticing.practicantes;
```

```java
// test/.../ModulosTest.java — prueba de arquitectura
@Test
void lasFronterasEntreModulosSeRespetan() {
    ApplicationModules.of(PracticantesApplication.class).verify();
}
```

Si alguien rompe una frontera, la prueba falla con un mensaje claro, por ejemplo:

```
Module 'seguridad' depends on module 'hojasvida' via ...seguridad.X -> ...hojasvida.internal.ValidadorHojaVida.
Allowed targets: compartido.
```

`ModulosTest` también genera los diagramas de módulos (PlantUML) en `backend/target/spring-modulith-docs/`.

Dentro de `practicantes`, el código conserva sus capas (`web` → `aplicacion` → `dominio` / `persistencia`), pero ninguna se ve desde fuera del módulo.

**¿Por qué monolito modular y no microservicios?** Da la misma separación de responsabilidades sin los costos de una red entre servicios: no hay latencia, no hay transacciones distribuidas y no hay que desplegar, versionar y monitorear varios servicios. Si un módulo creciera lo suficiente (por ejemplo, `hojasvida` con un almacenamiento en la nube y antivirus), ya tiene una frontera limpia para extraerlo como servicio.

### Patrones de diseño aplicados (y por qué)

Solo se aplicó un patrón cuando resolvía un problema concreto del proyecto. A continuación, cada patrón con el problema que resuelve y el fragmento de código donde se ve.

| # | Patrón | Dónde | Problema que resuelve |
|---|---|---|---|
| 1 | Monolito modular | `practicantes`, `hojasvida`, `seguridad`, `compartido` | Fronteras de negocio claras y verificadas, en un solo despliegue |
| 2 | Arquitectura en capas (dentro del módulo) | `practicantes.web` → `aplicacion` → `dominio` / `persistencia` | Cada capa tiene una sola responsabilidad |
| 3 | Facade | `hojasvida.HojasVidaService` | Una sola puerta de entrada al módulo; sus detalles quedan ocultos |
| 4 | Puerto y adaptador (Adapter) | `AlmacenamientoHojasVida` ← `AlmacenamientoLocalHojasVida` | El módulo no depende de dónde se guardan los PDF |
| 5 | Repository | `PracticanteRepository` | Separa el acceso a datos de la lógica de negocio |
| 6 | DTO | `RegistroPracticanteRequest`, `PracticanteResponse`, `ErrorResponse` | La API no expone la entidad ni datos internos |
| 7 | Método de fábrica + modelo de dominio rico | `Practicante.registrar(...)`, `Practicante.cambiarEstado(...)` | La entidad no puede quedar en un estado inválido |
| 8 | Inyección de dependencias | Constructores de servicios y controladores | Las clases se pueden probar con *mocks* |
| 9 | Jerarquía de errores + manejo centralizado | `ErrorDeNegocio`, `GlobalExceptionHandler` | Un único formato de error; una excepción nueva no obliga a tocar el manejador |

#### 1. Monolito modular

Explicado en la sección anterior: módulos por área de negocio, con fronteras que verifica `ModulosTest`.

#### 2. Arquitectura en capas (dentro del módulo `practicantes`)

**Por qué:** separar *cómo llega la petición* (HTTP), *qué hace el negocio* y *dónde se guardan los datos*. El controlador no tiene reglas de negocio; solo traduce HTTP a una llamada al servicio. La capa de aplicación tampoco depende de la web: recibe su propio `DatosRegistro`, no el DTO HTTP.

```java
// practicantes/web/PracticanteController.java — capa web: solo traduce HTTP ⇄ caso de uso
@PatchMapping("/{id}/estado")
public PracticanteResponse actualizarEstado(@PathVariable Long id,
                                            @Valid @RequestBody ActualizarEstadoRequest cuerpo) {
    return PracticanteResponse.desde(servicio.actualizarEstado(id, cuerpo.estado()));
}
```

```java
// practicantes/web/RegistroPracticanteRequest.java — la web traduce su DTO a la entrada del caso de uso
public DatosRegistro comoDatosRegistro() {
    return new DatosRegistro(nombreCompleto, correo, carrera, semestre);
}
```

#### 3. Facade — la API pública del módulo `hojasvida`

**Por qué:** guardar una hoja de vida implica tres pasos internos: validar que sea un PDF real, guardarla y limpiar el nombre original. Si `practicantes` coordinara esos tres pasos, conocería los detalles internos de otro módulo. Con la fachada, `practicantes` hace una sola llamada, y Spring Modulith impide que use las piezas internas.

```java
// hojasvida/HojasVidaService.java
@Service
public class HojasVidaService {

    public HojaVidaGuardada guardar(MultipartFile archivo) {
        validador.validar(archivo);                                   // hojasvida.internal
        try (InputStream contenido = archivo.getInputStream()) {
            String identificador = almacenamiento.guardar(contenido); // hojasvida.internal
            return new HojaVidaGuardada(identificador, NombresArchivo.paraMostrar(archivo.getOriginalFilename()));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la hoja de vida", e);
        }
    }

    public HojaVidaDescargable cargar(String identificador, String nombreParaMostrar) { /* ... */ }
    public void eliminar(String identificador) { /* ... */ }
}
```

```java
// practicantes/aplicacion/PracticanteService.java — una sola llamada al otro módulo
HojaVidaGuardada hojaVida = hojasVida.guardar(archivo);
```

#### 4. Puerto y adaptador (Adapter) — principio de inversión de dependencias (la D de SOLID)

**Por qué:** guardar los PDF en una carpeta local sirve para el reto, pero en un banco lo normal es moverlos a un almacenamiento en la nube (S3, Azure Blob). Dentro del módulo, la fachada depende de una **interfaz** (el puerto) y no de la carpeta (el adaptador). Cambiar a la nube sería escribir otro adaptador, sin tocar ni la fachada ni el módulo `practicantes`.

```java
// hojasvida/internal/AlmacenamientoHojasVida.java — el PUERTO: qué se necesita
public interface AlmacenamientoHojasVida {
    String guardar(InputStream contenido);
    Resource cargar(String identificador);
    void eliminar(String identificador);
}
```

```java
// hojasvida/internal/AlmacenamientoLocalHojasVida.java — el ADAPTADOR: cómo se hace hoy
@Component
public class AlmacenamientoLocalHojasVida implements AlmacenamientoHojasVida {

    @Override
    public String guardar(InputStream contenido) {
        String identificador = UUID.randomUUID() + EXTENSION;
        try {
            Files.copy(contenido, directorio.resolve(identificador));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la hoja de vida", e);
        }
        return identificador;
    }
    // cargar(...) y eliminar(...)
}
```

#### 5. Repository

**Por qué:** el servicio pide practicantes "por estado, del más reciente al más antiguo" sin escribir SQL ni saber que la base es SQLite. Spring Data genera la implementación a partir del nombre del método.

```java
// practicantes/persistencia/PracticanteRepository.java
public interface PracticanteRepository extends JpaRepository<Practicante, Long> {
    boolean existsByCorreoIgnoreCase(String correo);
    List<Practicante> findAllByOrderByFechaRegistroDesc();
    List<Practicante> findByEstadoOrderByFechaRegistroDesc(EstadoPracticante estado);
}
```

#### 6. DTO (Data Transfer Object)

**Por qué:** la entidad guarda el nombre interno del archivo en disco (`hojaVidaArchivo`, un UUID). Si la API devolviera la entidad directamente, expondría ese dato interno. El DTO decide qué sale y entrega en su lugar una URL de descarga.

```java
// practicantes/web/PracticanteResponse.java — lo que ve el front-end
public static PracticanteResponse desde(Practicante practicante) {
    return new PracticanteResponse(
            practicante.getId(),
            practicante.getNombreCompleto(),
            practicante.getCorreo(),
            practicante.getCarrera(),
            practicante.getSemestre(),
            practicante.getHojaVidaNombreOriginal(),
            Rutas.hojaVida(practicante.getId()),   // URL de descarga, nunca la ruta del disco
            practicante.getEstado(),
            practicante.getFechaRegistro(),
            practicante.getFechaActualizacionEstado());
}
```

#### 7. Método de fábrica + modelo de dominio rico (Experto en información, GRASP)

**Por qué:** con setters públicos, cualquier parte del código podía crear un practicante sin estado o cambiar el estado sin registrar la fecha. Ahora la entidad protege sus propias reglas:

- solo se crea con `registrar(...)`, que la deja en `PENDIENTE` y con el correo normalizado;
- el estado solo cambia con `cambiarEstado(...)`, que siempre registra la fecha;
- no tiene setters.

```java
// practicantes/dominio/Practicante.java
public static Practicante registrar(String nombreCompleto, String correo, String carrera, int semestre,
                                    String hojaVidaNombreOriginal, String hojaVidaArchivo) {
    return new Practicante(
            Objects.requireNonNull(nombreCompleto, "nombreCompleto").trim(),
            normalizarCorreo(correo),
            Objects.requireNonNull(carrera, "carrera").trim(),
            semestre,
            Objects.requireNonNull(hojaVidaNombreOriginal, "hojaVidaNombreOriginal"),
            Objects.requireNonNull(hojaVidaArchivo, "hojaVidaArchivo"));   // el constructor fija estado = PENDIENTE
}

public void cambiarEstado(EstadoPracticante nuevoEstado) {
    this.estado = Objects.requireNonNull(nuevoEstado, "El estado es obligatorio");
    this.fechaActualizacionEstado = LocalDateTime.now();   // estado y fecha cambian siempre juntos
}
```

El servicio queda como un coordinador muy corto:

```java
// practicantes/aplicacion/PracticanteService.java
@Transactional
public Practicante actualizarEstado(Long id, EstadoPracticante estado) {
    Practicante practicante = obtener(id);
    practicante.cambiarEstado(estado);
    return practicante; // entidad gestionada: JPA guarda el cambio al cerrar la transacción
}
```

#### 8. Inyección de dependencias por constructor

**Por qué:** las clases no crean lo que necesitan (`new ...`), lo reciben. Spring las conecta en producción, y en las pruebas se les pasan *mocks*. Así se prueba el módulo `practicantes` reemplazando el módulo `hojasvida` por un doble de su API, sin disco ni base de datos.

```java
// practicantes/aplicacion/PracticanteService.java
public PracticanteService(PracticanteRepository repositorio, HojasVidaService hojasVida) {
    this.repositorio = repositorio;
    this.hojasVida = hojasVida;
}
```

```java
// test/.../practicantes/aplicacion/PracticanteServiceTest.java
@Mock HojasVidaService hojasVida;

@Test
void registrar_falloInesperadoEnBd_eliminaArchivoYPropagaElError() {
    when(hojasVida.guardar(pdf)).thenReturn(GUARDADA);
    when(repositorio.saveAndFlush(any(Practicante.class))).thenThrow(new IllegalStateException("BD caída"));

    assertThatThrownBy(() -> servicio.registrar(datos, pdf)).isInstanceOf(IllegalStateException.class);
    verify(hojasVida).eliminar(ARCHIVO_INTERNO);   // no quedan archivos huérfanos
}
```

#### 9. Jerarquía de errores + manejo centralizado (principio abierto/cerrado, la O de SOLID)

**Por qué:** si el manejador global conociera las excepciones de cada módulo, `compartido` dependería de todos y todos de `compartido`: un ciclo que la prueba de arquitectura rechaza. La solución es una excepción base en `compartido`. Cada módulo la extiende, y el manejador solo conoce la base. Así, una excepción nueva no obliga a tocar el manejador. Además, el negocio no conoce códigos HTTP: dice qué *tipo* de error es (`TipoError`), y la capa web lo traduce.

```java
// compartido/ErrorDeNegocio.java
public abstract class ErrorDeNegocio extends RuntimeException {
    private final TipoError tipo;      // DATO_INVALIDO, CONFLICTO, NO_ENCONTRADO
    private final String campo;        // campo del formulario a marcar (opcional)
    // ...
}
```

```java
// practicantes/dominio/CorreoDuplicadoException.java — cada módulo define los suyos
public class CorreoDuplicadoException extends ErrorDeNegocio {
    public CorreoDuplicadoException() {
        super(TipoError.CONFLICTO, CAMPO, MENSAJE);
    }
}
```

```java
// compartido/web/GlobalExceptionHandler.java — un solo método para todos los errores de negocio
@ExceptionHandler(ErrorDeNegocio.class)
public ResponseEntity<ErrorResponse> errorDeNegocio(ErrorDeNegocio ex) {
    HttpStatus estado = estadoHttp(ex.getTipo());
    ErrorResponse error = ex.getCampo()
            .map(campo -> ErrorResponse.deCampo(estado, campo, ex.getMessage()))
            .orElseGet(() -> ErrorResponse.de(estado, ex.getMessage()));
    return responder(error);
}

static HttpStatus estadoHttp(TipoError tipo) {
    return switch (tipo) {
        case DATO_INVALIDO -> HttpStatus.BAD_REQUEST;
        case CONFLICTO -> HttpStatus.CONFLICT;
        case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
    };
}
```

Los errores que solo tienen sentido en un módulo los traduce el propio módulo (`practicantes.web.PracticantesExceptionHandler`, por ejemplo "Estados permitidos: PENDIENTE, VIABLE, NO_VIABLE"). Todos responden con el mismo JSON, incluido el login fallido de Spring Security:

```json
{ "estado": 409, "mensaje": "Ya existe un registro con este correo electrónico",
  "errores": { "correo": "Ya existe un registro con este correo electrónico" }, "fecha": "..." }
```

#### Principio de responsabilidad única (la S de SOLID)

Dentro del módulo `hojasvida`, cada clase tiene un único motivo para cambiar:

| Clase | Responsabilidad | Cambia si... |
|---|---|---|
| `ValidadorHojaVida` | Decidir si el archivo es un PDF aceptable | Cambian las reglas (por ejemplo, aceptar .docx) |
| `NombresArchivo` | Limpiar el nombre original para mostrarlo | Cambia cómo se muestran los nombres |
| `AlmacenamientoLocalHojasVida` | Guardar y leer archivos en disco | Cambia dónde se guardan |
| `HojasVidaService` | Coordinar lo anterior para otros módulos | Cambia lo que el módulo ofrece |

```java
// hojasvida/internal/ValidadorHojaVida.java — un PDF real empieza con "%PDF-", sin importar la extensión
private static boolean tieneFirmaPdf(MultipartFile archivo) {
    try (InputStream entrada = archivo.getInputStream()) {
        return Arrays.equals(entrada.readNBytes(FIRMA_PDF.length), FIRMA_PDF);
    } catch (IOException e) {
        return false;
    }
}
```

#### Patrones que aporta Spring (se usan, no se programaron)

| Patrón | Dónde aparece |
|---|---|
| Front Controller | `DispatcherServlet` recibe todas las peticiones y las reparte a los controladores |
| Chain of Responsibility | `SecurityFilterChain`: cada petición pasa por los filtros de CSRF, sesión y autorización |
| Strategy | `PasswordEncoder` (BCrypt) y los manejadores de login exitoso o fallido |
| Proxy | `@Transactional` y los repositorios de Spring Data funcionan con proxies |
| Unit of Work | JPA guarda los cambios de la entidad al cerrar la transacción |
| Builder | `ContentDisposition.attachment()...build()`, `User.withUsername(...)` |

#### Patrones descartados a propósito

| Patrón | Por qué no |
|---|---|
| Eventos entre módulos | Hoy ningún módulo necesita reaccionar a un registro. Si se agregan notificaciones por correo, `practicantes` publicaría un evento `PracticanteRegistrado` y el nuevo módulo lo escucharía, sin que se conozcan |
| Microservicios | Agregan red, despliegues y transacciones distribuidas sin un beneficio real para este tamaño |
| State | Cualquier estado puede pasar a cualquier otro; no hay transiciones que modelar |
| Builder propio | Los `record` de Java ya son inmutables y legibles |
| Chain of Responsibility para validar el PDF | Son tres reglas fijas; una cadena sería ceremonia sin beneficio |

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

Hay 56 pruebas, con **100 % de cobertura de líneas y ramas** en el back-end (JaCoCo; reporte en `backend/target/site/jacoco/index.html`). `mvn verify` **falla si la cobertura baja del 90 %**, así no puede degradarse sin que se note. Solo se excluye de la medición `PracticantesApplication` (el `main` que arranca Spring, sin lógica).

| Tipo | Clase | Qué cubre |
|---|---|---|
| **Arquitectura** | `ModulosTest` | Fronteras entre módulos, ausencia de ciclos, dependencias permitidas; genera los diagramas de módulos |
| Integración (API completa) | `PracticanteApiTest` | Flujo registrar → listar → marcar viable → descargar, campos vacíos, archivo no PDF, correo duplicado, acceso sin sesión, CSRF, estado inválido, 404, detalle, registro sin archivo, token CSRF, login correcto e incorrecto, health check |
| Unitaria (módulo practicantes) | `PracticanteServiceTest` | Registro, correo duplicado (también en condición de carrera), limpieza del archivo si falla la BD, cambio de estado; el módulo `hojasvida` se reemplaza por un *mock* |
| Unitaria (dominio) | `PracticanteTest` | Normalización de datos, estado inicial, fecha del cambio de estado |
| Módulo hojasvida (API pública) | `HojasVidaServiceTest` | Guardar, cargar y eliminar con una carpeta temporal; un archivo inválido no escribe nada en disco; fallo al releer el archivo |
| Unitaria (errores HTTP) | `GlobalExceptionHandlerTest` | Archivo demasiado grande (413, con el límite leído de la configuración) y código HTTP de cada tipo de error |
| Unitaria (internos de hojasvida) | `ValidadorHojaVidaTest`, `NombresArchivoTest`, `AlmacenamientoLocalHojasVidaTest` | Reglas del PDF, limpieza del nombre, bloqueo de *path traversal*, fallos de disco y de lectura |

## Calidad de código (SonarCloud)

El workflow `.github/workflows/sonarcloud.yml` corre en cada *push* a `main` y en cada *pull request*: ejecuta las pruebas, genera la cobertura y envía el análisis (Java + HTML/CSS/JS) a SonarCloud. La configuración está en `sonar-project.properties`.

Proyecto en SonarCloud: [AnaFiquitiva_kata-practicing](https://sonarcloud.io/project/overview?id=AnaFiquitiva_kata-practicing) (organización `anafiquitiva`).

Configuración necesaria (una sola vez):

1. En SonarCloud, *Administration → Analysis Method*: desactivar **Automatic Analysis**. El análisis automático no puede leer la cobertura; lo hace el workflow.
2. Generar un token en *My Account → Security* y guardarlo en GitHub como secreto `SONAR_TOKEN` (*Settings → Secrets and variables → Actions*, o `gh secret set SONAR_TOKEN`).

Se corrigieron los 4 hallazgos del primer análisis: promesas sin manejar en `analista.js` y `login.js`, una expresión regular del correo con riesgo de *backtracking* exponencial (ReDoS) en `registro.js`, y el contenedor del back-end que corría como `root`.

Mientras el secreto no exista, el workflow corre igual las pruebas y solo omite el análisis.

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
├── sonar-project.properties                # Configuración del análisis en SonarCloud
├── .github/workflows/sonarcloud.yml        # CI: pruebas + cobertura + SonarCloud
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
│       ├── PracticantesApplication.java
│       ├── practicantes/                   # MÓDULO: registro + decisión del analista
│       │   ├── web/                        #   PracticanteController, DTOs, errores del módulo
│       │   ├── aplicacion/                 #   PracticanteService (casos de uso)
│       │   ├── dominio/                    #   Practicante (con sus reglas), EstadoPracticante
│       │   └── persistencia/               #   PracticanteRepository (Spring Data JPA)
│       ├── hojasvida/                      # MÓDULO: validar, guardar y entregar PDF
│       │   ├── HojasVidaService.java       #   API pública (Facade)
│       │   └── internal/                   #   Validador, nombres y almacenamiento en disco (privados)
│       ├── seguridad/                      # MÓDULO: login/logout por API, CSRF, rutas protegidas
│       └── compartido/                     # MÓDULO: Rutas, ErrorResponse, ErrorDeNegocio
│           └── web/                        #   Manejador global de errores
├── backend_data/                           # Volúmenes persistentes (ignorado por git)
│   ├── db/
│   └── uploads/
└── docs/capturas/
```

## Decisiones de diseño y seguridad

- **Front y back separados.** El front no contiene lógica de servidor ni credenciales: solo consume la API. La autenticación ocurre en el back-end, y el panel redirige al login si `/api/auth/me` responde `401`.
- **Mismo origen gracias al proxy de Nginx.** No hay que abrir CORS y la cookie de sesión puede ser `HttpOnly`.
- Los PDF se guardan con nombre **UUID**; el nombre original solo se usa al descargar. Así no hay colisiones ni *path traversal*.
- Si falla el guardado en la base de datos, el archivo subido se elimina, de modo que no quedan archivos huérfanos. Si dos personas registran el mismo correo al mismo tiempo, la restricción `UNIQUE` de la BD decide y la API responde `409` (no `500`).
- El front construye el DOM con `textContent` (no `innerHTML`) para evitar XSS. Nginx envía una *Content Security Policy* estricta, `X-Frame-Options` y `X-Content-Type-Options`.
- Límite de subida de 5 MB, alineado en Spring (`multipart`) y en Nginx (`client_max_body_size`).
- El contenedor del back-end corre con un usuario sin privilegios (`app`, UID 1000), no como `root`.
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

### Panel del analista

Resumen por estado, lista con búsqueda y filtros, y detalle del practicante con los botones **Viable** / **No viable** y la descarga de la hoja de vida. Los datos de la captura son ficticios.

![Panel del analista](docs/capturas/02-panel-analista.png)

### Registro e inicio de sesión

| Registro | Login analista |
|---|---|
| ![Registro](docs/capturas/01-registro.png) | ![Login](docs/capturas/03-login.png) |

### Identidad visual

![Identidad visual y fuentes](docs/capturas/04-identidad-visual.png)
