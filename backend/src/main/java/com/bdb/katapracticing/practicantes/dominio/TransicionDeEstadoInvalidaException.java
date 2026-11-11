package com.bdb.katapracticing.practicantes.dominio;

import com.bdb.katapracticing.compartido.ErrorDeNegocio;
import com.bdb.katapracticing.compartido.TipoError;

/** El cambio pedido no es válido desde el estado actual (por ejemplo, VIABLE → VIABLE). */
public class TransicionDeEstadoInvalidaException extends ErrorDeNegocio {

    private static final String CAMPO = "estado";

    public TransicionDeEstadoInvalidaException(EstadoPracticante actual, EstadoPracticante destino) {
        super(TipoError.CONFLICTO, CAMPO,
                "El practicante ya está en estado " + actual + "; no se puede cambiar a " + destino);
    }
}
