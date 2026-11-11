// `void`: la promesa de arranque se ignora a propósito; sus errores se manejan dentro (try/catch final).
void (async () => {
  const estado = { practicantes: [], filtro: "", busqueda: "", seleccionado: null, csrf: null };

  const $ = (id) => document.getElementById(id);
  const cuerpoTabla = $("tabla-cuerpo");
  const alerta = $("alerta");

  const DURACION_ALERTA_MS = 4000;
  const avisar = (tipo, texto) => mostrarAlerta(alerta, tipo, texto, DURACION_ALERTA_MS);

  /** Error de la API con su código HTTP (para distinguir, por ejemplo, 412 de un error genérico). */
  class ErrorApi extends Error {
    constructor(mensaje, estadoHttp) {
      super(mensaje);
      this.estadoHttp = estadoHttp;
    }
  }

  // Ruta permitida: /api seguido de segmentos simples y, opcionalmente, una query (?page=1&size=100&sort=...).
  const RUTA_API = /^\/api(?:\/[A-Za-z0-9_-]+)*\/?(?:\?[A-Za-z0-9_=&,%.-]*)?$/;

  /**
   * El panel sigue los enlaces (_links) que entrega la API, pero solo si apuntan a esta misma API:
   * mismo origen y ruta bajo /api. Así un enlace manipulado no puede hacer que el navegador
   * envíe peticiones (con la cookie de sesión) a otro sitio o a otra ruta (request forgery).
   */
  function rutaDeLaApi(url) {
    const destino = new URL(url, location.origin);
    const ruta = destino.pathname + destino.search;
    if (destino.origin !== location.origin || !RUTA_API.test(ruta)) {
      throw new ErrorApi("La API devolvió un enlace no permitido", 0);
    }
    return ruta;
  }

  async function api(url, opciones = {}) {
    const res = await fetch(rutaDeLaApi(url), { credentials: "same-origin", ...opciones });
    if (res.status === 401 || res.status === 403) {
      location.href = "/login.html";
      throw new ErrorApi("Sesión expirada", res.status);
    }
    const cuerpo = await res.json().catch(() => ({}));
    if (!res.ok) throw new ErrorApi(cuerpo.mensaje || "Error inesperado", res.status);
    return cuerpo;
  }

  // --- HATEOAS: el cliente no arma URLs; sigue los enlaces (_links) que entrega la API. ---
  const enlace = (recurso, rel) => recurso?._links?.[rel];
  const REL_ACCION = { PENDIENTE: "marcar-pendiente", VIABLE: "marcar-viable", NO_VIABLE: "marcar-no-viable" };

  function badge(est) {
    return el("span", { class: `estado estado--${est}` }, ETIQUETA_ESTADO[est] || est);
  }

  function actualizarResumen() {
    const conteo = { PENDIENTE: 0, VIABLE: 0, NO_VIABLE: 0 };
    estado.practicantes.forEach(p => conteo[p.estado]++);
    $("cnt-total").textContent = estado.practicantes.length;
    Object.entries(conteo).forEach(([k, v]) => ($(`cnt-${k}`).textContent = v));
  }

  function renderTabla() {
    const q = estado.busqueda.toLowerCase();
    const visibles = estado.practicantes.filter(p =>
      (!estado.filtro || p.estado === estado.filtro) &&
      (!q || [p.nombreCompleto, p.correo, p.carrera].some(v => v.toLowerCase().includes(q))));

    cuerpoTabla.replaceChildren(...visibles.map(p => el("tr",
      {
        class: p.id === estado.seleccionado ? "seleccionado" : "",
        tabindex: "0",
        onclick: () => seleccionar(p.id),
        onkeydown: (e) => { if (e.key === "Enter") seleccionar(p.id); },
      },
      el("td", {}, el("div", {}, p.nombreCompleto), el("div", { class: "secundario" }, p.correo)),
      el("td", {}, p.carrera),
      el("td", {}, p.semestre),
      el("td", {}, badge(p.estado)),
    )));
    $("sin-resultados").hidden = visibles.length > 0;
  }

  function renderDetalle() {
    const p = estado.practicantes.find(x => x.id === estado.seleccionado);
    $("detalle-vacio").hidden = Boolean(p);
    $("detalle-contenido").hidden = !p;
    if (!p) return;
    $("d-nombre").textContent = p.nombreCompleto;
    $("d-correo").textContent = p.correo;
    $("d-carrera").textContent = p.carrera;
    $("d-semestre").textContent = `${p.semestre}.º`;
    $("d-fecha").textContent = formatearFecha(p.fechaRegistro);
    $("d-estado").replaceChildren(badge(p.estado));
    $("d-hv").textContent = p.hojaVidaNombre;
    $("d-descargar").href = enlace(p, "hoja-vida")?.href ?? "#";
    // Un botón solo se habilita si la API ofrece el enlace de esa transición para el estado actual.
    document.querySelectorAll("[data-accion]").forEach(b => (b.disabled = !enlace(p, REL_ACCION[b.dataset.accion])));
  }

  function seleccionar(id) {
    estado.seleccionado = id;
    renderTabla();
    renderDetalle();
    if (window.matchMedia("(max-width: 900px)").matches) $("detalle").scrollIntoView({ behavior: "smooth" });
  }

  /** Descubre la colección desde la raíz de la API y recorre todas las páginas siguiendo el enlace "next". */
  async function cargar() {
    const raiz = await api("/api");
    let url = enlace(raiz, "practicantes").href.replace(/\{\?[^}]*\}$/, "") + "?size=100";
    const todos = [];
    while (url) {
      const pagina = await api(url);
      todos.push(...(pagina._embedded?.practicantes ?? []));
      url = enlace(pagina, "next")?.href;
    }
    estado.practicantes = todos;
    actualizarResumen();
    renderTabla();
    renderDetalle();
  }

  async function cambiarEstado(nuevo) {
    const id = estado.seleccionado;
    const actual = estado.practicantes.find(p => p.id === id);
    const accion = enlace(actual, REL_ACCION[nuevo]);
    if (!accion) return;
    const botones = document.querySelectorAll("[data-accion]");
    botones.forEach(b => (b.disabled = true));
    try {
      const actualizado = await api(accion.href, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          // Concurrencia optimista: si otro analista lo cambió primero, la API responde 412.
          "If-Match": `"${actual.version}"`,
          [estado.csrf.headerName]: estado.csrf.token,
        },
        body: JSON.stringify({ estado: accion.name }),
      });
      estado.practicantes = estado.practicantes.map(p => (p.id === id ? actualizado : p));
      actualizarResumen();
      renderTabla();
      avisar("exito", `${actualizado.nombreCompleto} quedó marcado como "${ETIQUETA_ESTADO[nuevo]}".`);
    } catch (e) {
      avisar("error", e.message);
      // 409/412: el registro cambió en el servidor; se recarga para mostrar su estado real.
      if (e.estadoHttp === 409 || e.estadoHttp === 412) await cargar().catch(() => {});
    } finally {
      renderDetalle();
    }
  }

  // --- Eventos ---
  $("buscar").addEventListener("input", (e) => { estado.busqueda = e.target.value; renderTabla(); });
  $("filtros").addEventListener("click", (e) => {
    const b = e.target.closest(".filtro");
    if (!b) return;
    document.querySelectorAll(".filtro").forEach(f => f.classList.toggle("activo", f === b));
    estado.filtro = b.dataset.estado;
    renderTabla();
  });
  document.querySelectorAll("[data-accion]").forEach(b =>
    b.addEventListener("click", () => cambiarEstado(b.dataset.accion)));

  $("btn-salir").addEventListener("click", async () => {
    try {
      await fetch("/api/auth/logout", {
        method: "POST",
        headers: { [estado.csrf.headerName]: estado.csrf.token },
        credentials: "same-origin",
      });
    } finally {
      location.href = "/login.html?salida";
    }
  });

  // --- Inicio ---
  try {
    const sesion = await api("/api/auth/me");
    $("usuario-actual").textContent = `Analista: ${sesion.usuario}`;
    estado.csrf = await obtenerCsrf();
    await cargar();
  } catch (e) {
    avisar("error", e.message);
  }
})();
