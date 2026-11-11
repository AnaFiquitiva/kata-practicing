package com.bdb.katapracticing.seguridad;

import com.bdb.katapracticing.compartido.Rutas;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de sesión. El login y el logout los atiende Spring Security directamente
 * (ver {@link com.bdb.katapracticing.seguridad.SecurityConfig}).
 */
@RestController
public class SesionController {

    /** Entrega el token CSRF al front-end para que pueda hacer POST/PATCH. */
    @GetMapping(Rutas.CSRF)
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    /** Analista autenticado. Responde 401 si no hay sesión. */
    @GetMapping(Rutas.USUARIO_ACTUAL)
    public Map<String, String> usuarioActual(Authentication autenticacion) {
        return Map.of("usuario", autenticacion.getName());
    }
}
