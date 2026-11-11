package com.bdb.katapracticing.hojasvida;

import com.bdb.katapracticing.compartido.ErrorDeNegocio;
import com.bdb.katapracticing.compartido.TipoError;

public class ArchivoInvalidoException extends ErrorDeNegocio {

    /** Campo del formulario de registro que el front marca en rojo. */
    public static final String CAMPO = "hojaVida";

    public ArchivoInvalidoException(String mensaje) {
        super(TipoError.DATO_INVALIDO, CAMPO, mensaje);
    }
}
