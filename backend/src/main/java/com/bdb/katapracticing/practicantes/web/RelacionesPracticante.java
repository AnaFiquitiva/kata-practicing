package com.bdb.katapracticing.practicantes.web;

import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import org.springframework.hateoas.LinkRelation;

import java.util.Locale;

/**
 * Relaciones (rel) de los enlaces de un practicante. Son el contrato con el cliente:
 * el cliente busca el enlace por su rel y usa su href, sin construir URLs a mano.
 */
public final class RelacionesPracticante {

    public static final LinkRelation HOJA_VIDA = LinkRelation.of("hoja-vida");
    public static final LinkRelation FILTRAR_POR_ESTADO = LinkRelation.of("filtrar-por-estado");
    public static final LinkRelation RAIZ = LinkRelation.of("raiz");

    private RelacionesPracticante() {
    }

    /** marcar-pendiente, marcar-viable o marcar-no-viable. */
    public static LinkRelation marcar(EstadoPracticante destino) {
        return LinkRelation.of("marcar-" + destino.name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }

    static String titulo(EstadoPracticante destino) {
        return switch (destino) {
            case PENDIENTE -> "Regresar a pendiente";
            case VIABLE -> "Marcar como viable";
            case NO_VIABLE -> "Marcar como no viable";
        };
    }
}
