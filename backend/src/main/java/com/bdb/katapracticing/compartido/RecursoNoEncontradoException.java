package com.bdb.katapracticing.compartido;

public class RecursoNoEncontradoException extends ErrorDeNegocio {

    public RecursoNoEncontradoException(String mensaje) {
        super(TipoError.NO_ENCONTRADO, mensaje);
    }
}
