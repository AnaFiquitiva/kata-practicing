package com.bdb.katapracticing.compartido;

/** Un parámetro de la petición no es válido (responde 400 asociado al campo). */
public class DatoInvalidoException extends ErrorDeNegocio {

    public DatoInvalidoException(String campo, String mensaje) {
        super(TipoError.DATO_INVALIDO, campo, mensaje);
    }
}
