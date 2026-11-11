package com.bdb.katapracticing.compartido;

/** Rutas públicas de la API en un solo lugar (controladores, seguridad y documentación las comparten). */
public final class Rutas {

    /** Punto de entrada de la API (HATEOAS): desde aquí el cliente descubre el resto por enlaces. */
    public static final String API = "/api";
    public static final String CSRF = API + "/csrf";
    public static final String LOGIN = API + "/auth/login";
    public static final String LOGOUT = API + "/auth/logout";
    public static final String USUARIO_ACTUAL = API + "/auth/me";
    public static final String PRACTICANTES = API + "/practicantes";
    public static final String HEALTH = "/actuator/health";

    /** Documentación: bajo /api para que también se sirva a través del proxy Nginx (localhost:3000). */
    public static final String DOCS = API + "/docs";
    public static final String OPENAPI = DOCS + "/openapi";
    public static final String SWAGGER_UI = DOCS + "/swagger-ui.html";

    private Rutas() {
    }
}
