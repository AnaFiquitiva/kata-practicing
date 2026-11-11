package com.bdb.katapracticing.compartido;

/** Categorías de error del negocio. El módulo web decide qué código HTTP corresponde a cada una. */
public enum TipoError {
    DATO_INVALIDO,
    CONFLICTO,
    NO_ENCONTRADO,
    PRECONDICION_FALLIDA
}
