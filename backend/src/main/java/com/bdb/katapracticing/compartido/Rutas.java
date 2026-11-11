package com.bdb.katapracticing.compartido;

/** Rutas públicas de la API en un solo lugar (controladores, seguridad y DTOs las comparten). */
public final class Rutas {

    public static final String API = "/api";
    public static final String CSRF = API + "/csrf";
    public static final String LOGIN = API + "/auth/login";
    public static final String LOGOUT = API + "/auth/logout";
    public static final String USUARIO_ACTUAL = API + "/auth/me";
    public static final String PRACTICANTES = API + "/practicantes";
    public static final String HEALTH = "/actuator/health";

    private Rutas() {
    }

    public static String hojaVida(long idPracticante) {
        return PRACTICANTES + "/" + idPracticante + "/hoja-vida";
    }
}
