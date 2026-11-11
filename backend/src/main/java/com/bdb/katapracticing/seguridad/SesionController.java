package com.bdb.katapracticing.seguridad;

import com.bdb.katapracticing.compartido.Rutas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de sesión. El login y el logout los atiende Spring Security directamente
 * (ver {@link com.bdb.katapracticing.seguridad.SecurityConfig}).
 */
@Tag(name = "Sesión", description = "Autenticación del analista y token CSRF")
@RestController
public class SesionController {

    /** Entrega el token CSRF al front-end para que pueda hacer POST/PATCH. */
    @Operation(summary = "Obtener el token CSRF (público)",
            description = "Devuelve el token y el nombre de la cabecera en la que se debe enviar en POST/PATCH")
    @GetMapping(Rutas.CSRF)
    public Map<String, String> csrf(@Parameter(hidden = true) CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    /** Analista autenticado. Responde 401 si no hay sesión. */
    @Operation(summary = "Analista autenticado", security = @SecurityRequirement(name = "sesion"))
    @GetMapping(Rutas.USUARIO_ACTUAL)
    public Map<String, String> usuarioActual(@Parameter(hidden = true) Authentication autenticacion) {
        return Map.of("usuario", autenticacion.getName());
    }
}
