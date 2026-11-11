// Utilidades compartidas por las páginas.

/** Obtiene el token CSRF que Spring Security exige para POST/PATCH. */
async function obtenerCsrf() {
  const res = await fetch("/api/csrf", { credentials: "same-origin" });
  if (!res.ok) throw new Error("No se pudo obtener el token de seguridad");
  return res.json(); // { token, headerName }
}

/** Crea un elemento DOM de forma segura (sin innerHTML → sin XSS). */
function el(tag, attrs = {}, ...hijos) {
  const nodo = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k === "class") nodo.className = v;
    else if (k.startsWith("on")) nodo.addEventListener(k.slice(2), v);
    else nodo.setAttribute(k, v);
  }
  for (const h of hijos.flat()) {
    if (h == null) continue;
    nodo.append(h instanceof Node ? h : document.createTextNode(String(h)));
  }
  return nodo;
}

const temporizadoresAlerta = new WeakMap();

/**
 * Muestra un mensaje en el contenedor de alertas de la página.
 * @param {HTMLElement} contenedor  elemento .alerta
 * @param {"exito"|"error"|"info"} tipo
 * @param {string} texto
 * @param {number} [autoOcultarMs]  si se indica, la alerta se oculta sola
 */
function mostrarAlerta(contenedor, tipo, texto, autoOcultarMs = 0) {
  contenedor.className = `alerta alerta--${tipo}`;
  contenedor.textContent = texto;
  contenedor.hidden = false;
  clearTimeout(temporizadoresAlerta.get(contenedor));
  if (autoOcultarMs > 0) {
    temporizadoresAlerta.set(contenedor, setTimeout(() => { contenedor.hidden = true; }, autoOcultarMs));
  }
}

function formatearFecha(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("es-CO", { dateStyle: "medium", timeStyle: "short" });
}

const ETIQUETA_ESTADO = { PENDIENTE: "Pendiente", VIABLE: "Viable", NO_VIABLE: "No viable" };
