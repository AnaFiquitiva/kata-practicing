(() => {
  const form = document.getElementById("form-registro");
  const alerta = document.getElementById("alerta");
  const boton = document.getElementById("btn-enviar");
  const selectSemestre = document.getElementById("semestre");
  const MAX_BYTES = 5 * 1024 * 1024;
  const SEMESTRE_MAXIMO = 10; // mismo límite que valida la API (Practicante.SEMESTRE_MAXIMO)

  /**
   * usuario@dominio.tld, sin espacios, con un solo "@", etiquetas de dominio no vacías y TLD de 2+ letras.
   * Se valida sin expresión regular para evitar backtracking exponencial (ReDoS). El servidor vuelve a validar.
   */
  function esCorreoValido(correo) {
    if (/\s/.test(correo)) return false;
    const partes = correo.split("@");
    if (partes.length !== 2 || !partes[0]) return false;
    const etiquetas = partes[1].split(".");
    return etiquetas.length >= 2 && etiquetas.every(Boolean) && etiquetas.at(-1).length >= 2;
  }

  // Las opciones (1 a 10) están en el HTML. Por si el HTML llegara con opciones de más, se retiran las inválidas.
  [...selectSemestre.options].filter(o => o.value && Number(o.value) > SEMESTRE_MAXIMO).forEach(o => o.remove());

  function marcarError(campo, mensaje) {
    const contenedor = form.querySelector(`[name="${campo}"]`)?.closest(".campo");
    const destino = form.querySelector(`[data-error-de="${campo}"]`);
    if (contenedor) contenedor.classList.toggle("invalido", Boolean(mensaje));
    if (destino) destino.textContent = mensaje || "";
  }

  function validar() {
    const d = new FormData(form);
    const errores = {};
    const nombre = (d.get("nombreCompleto") || "").trim();
    const correo = (d.get("correo") || "").trim();
    const carrera = (d.get("carrera") || "").trim();
    const semestre = d.get("semestre");
    const archivo = form.hojaVida.files[0];

    if (!nombre) errores.nombreCompleto = "El nombre completo es obligatorio";
    if (!correo) errores.correo = "El correo electrónico es obligatorio";
    else if (!esCorreoValido(correo)) errores.correo = "Ingresa un correo electrónico válido";
    if (!carrera) errores.carrera = "La carrera universitaria es obligatoria";
    if (!semestre) errores.semestre = "Selecciona tu semestre actual";
    else if (Number(semestre) < 1 || Number(semestre) > SEMESTRE_MAXIMO) {
      errores.semestre = `El semestre debe estar entre 1 y ${SEMESTRE_MAXIMO}`;
    }
    if (!archivo) errores.hojaVida = "Adjunta tu hoja de vida en PDF";
    else if (!archivo.name.toLowerCase().endsWith(".pdf")) errores.hojaVida = "El archivo debe ser PDF";
    else if (archivo.size > MAX_BYTES) errores.hojaVida = "El archivo no puede superar 5 MB";

    ["nombreCompleto", "correo", "carrera", "semestre", "hojaVida"].forEach(c => marcarError(c, errores[c]));
    return errores;
  }

  // Validación en vivo cuando el usuario sale de un campo ya tocado.
  form.addEventListener("change", () => { if (form.dataset.intentado) validar(); });

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    form.dataset.intentado = "1";
    alerta.hidden = true;

    const errores = validar();
    if (Object.keys(errores).length) {
      mostrarAlerta(alerta, "error", "Revisa los campos marcados en rojo.");
      form.querySelector(".campo.invalido input, .campo.invalido select")?.focus();
      return;
    }

    boton.disabled = true;
    boton.textContent = "Enviando…";
    try {
      const csrf = await obtenerCsrf();
      const res = await fetch("/api/practicantes", {
        method: "POST",
        body: new FormData(form),
        headers: { [csrf.headerName]: csrf.token },
        credentials: "same-origin",
      });
      const cuerpo = await res.json().catch(() => ({}));

      if (res.ok) {
        form.reset();
        delete form.dataset.intentado;
        mostrarAlerta(alerta, "exito", `¡Gracias, ${cuerpo.nombreCompleto}! Tu registro fue recibido correctamente.`);
      } else {
        Object.entries(cuerpo.errores || {}).forEach(([c, m]) => marcarError(c, m));
        mostrarAlerta(alerta, "error", cuerpo.mensaje || "No fue posible enviar el registro.");
      }
    } catch {
      mostrarAlerta(alerta, "error", "No hay conexión con el servidor. Intenta de nuevo.");
    } finally {
      boton.disabled = false;
      boton.textContent = "Enviar registro";
      alerta.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  });
})();
