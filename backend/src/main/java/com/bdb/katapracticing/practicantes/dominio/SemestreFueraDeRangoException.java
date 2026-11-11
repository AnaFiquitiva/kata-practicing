package com.bdb.katapracticing.practicantes.dominio;

import com.bdb.katapracticing.compartido.ErrorDeNegocio;
import com.bdb.katapracticing.compartido.TipoError;

/** El semestre está fuera del rango permitido (ver {@link Practicante#SEMESTRE_MAXIMO}). */
public class SemestreFueraDeRangoException extends ErrorDeNegocio {

    public static final String CAMPO = "semestre";
    public static final String MENSAJE = "El semestre debe estar entre "
            + Practicante.SEMESTRE_MINIMO + " y " + Practicante.SEMESTRE_MAXIMO;

    public SemestreFueraDeRangoException() {
        super(TipoError.DATO_INVALIDO, CAMPO, MENSAJE);
    }
}
