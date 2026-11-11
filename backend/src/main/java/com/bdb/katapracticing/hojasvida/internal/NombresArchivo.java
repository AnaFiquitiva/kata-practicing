package com.bdb.katapracticing.hojasvida.internal;

import java.util.regex.Pattern;

/** Limpia el nombre original del archivo para mostrarlo y usarlo en la descarga. */
public final class NombresArchivo {

    static final String NOMBRE_POR_DEFECTO = "hoja-de-vida.pdf";
    static final int LONGITUD_MAXIMA = 150;
    private static final Pattern CARACTERES_NO_PERMITIDOS = Pattern.compile("[\\p{Cntrl}\"]");

    private NombresArchivo() {
    }

    /** Quita rutas que envían algunos navegadores (C:\fakepath\...), caracteres de control y comillas. */
    public static String paraMostrar(String original) {
        if (original == null || original.isBlank()) {
            return NOMBRE_POR_DEFECTO;
        }
        String sinRuta = original.replace('\\', '/');
        sinRuta = sinRuta.substring(sinRuta.lastIndexOf('/') + 1);
        String limpio = CARACTERES_NO_PERMITIDOS.matcher(sinRuta).replaceAll("").trim();
        if (limpio.isEmpty()) {
            return NOMBRE_POR_DEFECTO;
        }
        // Si es muy largo se conserva el final, que es donde está la extensión.
        return limpio.length() > LONGITUD_MAXIMA ? limpio.substring(limpio.length() - LONGITUD_MAXIMA) : limpio;
    }
}
