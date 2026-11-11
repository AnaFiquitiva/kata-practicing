package com.bdb.katapracticing.compartido;

/**
 * Traduce la versión de un recurso a una ETag fuerte ({@code "3"}) y viceversa.
 * La usan las peticiones condicionales: {@code If-None-Match} (caché) e {@code If-Match} (concurrencia optimista).
 */
public final class ETags {

    private static final String COMODIN = "*";
    private static final String PREFIJO_DEBIL = "W/";

    private ETags() {
    }

    public static String de(long version) {
        return "\"" + version + "\"";
    }

    /**
     * Versión que el cliente envía en {@code If-Match}, o {@code null} si no envió la cabecera
     * o envió {@code *} (cualquier versión).
     *
     * @throws PrecondicionFallidaException si la ETag no corresponde a ninguna versión posible
     */
    public static Long versionDe(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank() || COMODIN.equals(ifMatch.trim())) {
            return null;
        }
        try {
            return Long.parseLong(limpiar(ifMatch));
        } catch (NumberFormatException e) {
            throw new PrecondicionFallidaException();
        }
    }

    /**
     * ¿Alguna de las ETags de {@code If-None-Match} corresponde a la versión actual? Si es así, el cliente
     * ya tiene la representación vigente y se responde 304. Acepta varias ETags separadas por coma,
     * ETags débiles ({@code W/"3"}) y el comodín {@code *}.
     */
    public static boolean coincide(String ifNoneMatch, long version) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
            return false;
        }
        String actual = String.valueOf(version);
        for (String etag : ifNoneMatch.split(",")) {
            if (COMODIN.equals(etag.trim()) || actual.equals(limpiar(etag))) {
                return true;
            }
        }
        return false;
    }

    /** Quita espacios, el prefijo débil {@code W/} y las comillas: {@code W/"3"} → {@code 3}. */
    private static String limpiar(String etag) {
        String valor = etag.trim();
        if (valor.startsWith(PREFIJO_DEBIL)) {
            valor = valor.substring(PREFIJO_DEBIL.length());
        }
        return valor.replace("\"", "");
    }
}
