package com.bdb.katapracticing.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de sesión. El login (POST /api/auth/login) y el logout (POST /api/auth/logout)
 * los atiende Spring Security directamente (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api")
public class SesionController {

    /** Entrega el token CSRF al front-end para que pueda hacer POST/PATCH. */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    /** Analista autenticado. Responde 401 si no hay sesión. */
    @GetMapping("/auth/me")
    public Map<String, String> yo(Authentication auth) {
        return Map.of("usuario", auth.getName());
    }
}
