package com.bdb.katapracticing.compartido;

/**
 * La versión que el cliente envió en {@code If-Match} ya no es la actual: alguien modificó
 * el recurso desde que el cliente lo leyó. Responde 412 para evitar la "actualización perdida".
 */
public class PrecondicionFallidaException extends ErrorDeNegocio {

    public static final String MENSAJE =
            "El registro cambió desde que lo consultaste. Vuelve a cargarlo e intenta de nuevo";

    public PrecondicionFallidaException() {
        super(TipoError.PRECONDICION_FALLIDA, MENSAJE);
    }
}
