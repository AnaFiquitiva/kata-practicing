(async () => {
  const estado = { practicantes: [], filtro: "", busqueda: "", seleccionado: null, csrf: null };

  const $ = (id) => document.getElementById(id);
  const cuerpoTabla = $("tabla-cuerpo");
  const alerta = $("alerta");

  function mostrarAlerta(tipo, texto) {
    alerta.className = `alerta alerta--${tipo}`;
    alerta.textContent = texto;
    alerta.hidden = false;
    clearTimeout(mostrarAlerta.t);
    mostrarAlerta.t = setTimeout(() => (alerta.hidden = true), 4000);
  }

  async function api(url, opciones = {}) {
    const res = await fetch(url, { credentials: "same-origin", ...opciones });
    if (res.status === 401 || res.status === 403) {
      location.href = "/login.html";
      throw new Error("Sesión expirada");
    }
    const cuerpo = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(cuerpo.mensaje || "Error inesperado");
    return cuerpo;
  }

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
    $("d-descargar").href = p.hojaVidaUrl;
    document.querySelectorAll("[data-accion]").forEach(b => (b.disabled = b.dataset.accion === p.estado));
  }

  function seleccionar(id) {
    estado.seleccionado = id;
    renderTabla();
    renderDetalle();
    if (window.matchMedia("(max-width: 900px)").matches) $("detalle").scrollIntoView({ behavior: "smooth" });
  }

  async function cargar() {
    estado.practicantes = await api("/api/practicantes");
    actualizarResumen();
    renderTabla();
    renderDetalle();
  }

  async function cambiarEstado(nuevo) {
    const id = estado.seleccionado;
    const botones = document.querySelectorAll("[data-accion]");
    botones.forEach(b => (b.disabled = true));
    try {
      const actualizado = await api(`/api/practicantes/${id}/estado`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json", [estado.csrf.headerName]: estado.csrf.token },
        body: JSON.stringify({ estado: nuevo }),
      });
      estado.practicantes = estado.practicantes.map(p => (p.id === id ? actualizado : p));
      actualizarResumen();
      renderTabla();
      mostrarAlerta("exito", `${actualizado.nombreCompleto} quedó marcado como "${ETIQUETA_ESTADO[nuevo]}".`);
    } catch (e) {
      mostrarAlerta("error", e.message);
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
    mostrarAlerta("error", e.message);
  }
})();
