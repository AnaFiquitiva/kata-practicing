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
        String valor = ifMatch.trim();
        if (valor.startsWith(PREFIJO_DEBIL)) {
            valor = valor.substring(PREFIJO_DEBIL.length());
        }
        valor = valor.replace("\"", "");
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            throw new PrecondicionFallidaException();
        }
    }
}
