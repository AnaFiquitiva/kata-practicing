package com.bdb.katapracticing.practicantes.dominio;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados del proceso de selección. Cada estado sabe a cuáles puede pasar: el dominio decide
 * las transiciones válidas y la capa web solo las publica como enlaces (HATEOAS).
 */
public enum EstadoPracticante {
    PENDIENTE,
    VIABLE,
    NO_VIABLE;

    /** Un practicante puede pasar a cualquier otro estado, pero no "cambiar" al que ya tiene. */
    public Set<EstadoPracticante> transicionesPermitidas() {
        return EnumSet.complementOf(EnumSet.of(this));
    }

    public boolean puedeCambiarA(EstadoPracticante destino) {
        return destino != null && transicionesPermitidas().contains(destino);
    }
}
