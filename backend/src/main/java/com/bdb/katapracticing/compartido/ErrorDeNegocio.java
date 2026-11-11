package com.bdb.katapracticing.compartido;

import java.util.Optional;

/**
 * Base de los errores que cualquier módulo puede lanzar. Gracias a ella, el manejador global
 * no necesita conocer las excepciones de cada módulo (lo que crearía dependencias en ciclo),
 * y una excepción nueva no obliga a tocar el manejador (principio abierto/cerrado).
 */
public abstract class ErrorDeNegocio extends RuntimeException {

    private final TipoError tipo;
    private final String campo;

    protected ErrorDeNegocio(TipoError tipo, String mensaje) {
        this(tipo, null, mensaje);
    }

    /** @param campo campo del formulario al que se asocia el error (lo usa el front para marcarlo). */
    protected ErrorDeNegocio(TipoError tipo, String campo, String mensaje) {
        super(mensaje);
        this.tipo = tipo;
        this.campo = campo;
    }

    public TipoError getTipo() {
        return tipo;
    }

    public Optional<String> getCampo() {
        return Optional.ofNullable(campo);
    }
}
