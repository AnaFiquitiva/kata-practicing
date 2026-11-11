package com.bdb.katapracticing.practicantes.dominio;

import com.bdb.katapracticing.compartido.ErrorDeNegocio;
import com.bdb.katapracticing.compartido.TipoError;

public class CorreoDuplicadoException extends ErrorDeNegocio {

    public static final String MENSAJE = "Ya existe un registro con este correo electrónico";
    private static final String CAMPO = "correo";

    public CorreoDuplicadoException() {
        super(TipoError.CONFLICTO, CAMPO, MENSAJE);
    }
}
