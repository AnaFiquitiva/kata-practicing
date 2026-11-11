// Inicio de sesión del analista contra la API (POST /api/auth/login → JSON).
(async () => {
  const alerta = document.getElementById("alerta");
  const form = document.getElementById("form-login");
  const boton = document.getElementById("btn-ingresar");
  const params = new URLSearchParams(location.search);

  function mostrar(tipo, texto) {
    alerta.className = `alerta alerta--${tipo}`;
    alerta.textContent = texto;
    alerta.hidden = false;
  }

  if (params.has("salida")) mostrar("info", "Cerraste sesión correctamente.");

  // Si ya hay sesión activa, ir directo al panel.
  try {
    const yo = await fetch("/api/auth/me", { credentials: "same-origin" });
    if (yo.ok) { location.replace("/analista.html"); return; }
  } catch { /* sin conexión: se informa al enviar */ }

  boton.disabled = false;

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const usuario = form.username.value.trim();
    const clave = form.password.value;
    if (!usuario || !clave) {
      mostrar("error", "Ingresa usuario y contraseña.");
      return;
    }

    boton.disabled = true;
    boton.textContent = "Ingresando…";
    try {
      const csrf = await obtenerCsrf();
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { [csrf.headerName]: csrf.token },
        body: new URLSearchParams({ username: usuario, password: clave }),
        credentials: "same-origin",
      });
      if (res.ok) {
        location.replace("/analista.html");
        return;
      }
      const cuerpo = await res.json().catch(() => ({}));
      mostrar("error", cuerpo.mensaje || "Usuario o contraseña incorrectos.");
      form.password.value = "";
      form.password.focus();
    } catch {
      mostrar("error", "No hay conexión con el servidor.");
    } finally {
      boton.disabled = false;
      boton.textContent = "Ingresar";
    }
  });
})();
