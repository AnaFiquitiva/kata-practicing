package com.bdb.katapracticing.practicantes.web;

import com.bdb.katapracticing.compartido.Rutas;
import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import com.bdb.katapracticing.practicantes.dominio.Practicante;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Set;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Convierte un {@link Practicante} en su representación hipermedia (REST nivel 3).
 * Los enlaces dependen del estado: solo se ofrecen las transiciones que el dominio permite,
 * así el cliente no necesita conocer las reglas para saber qué botones habilitar.
 */
@Component
public class PracticanteModelAssembler
        implements RepresentationModelAssembler<Practicante, EntityModel<PracticanteResponse>> {

    @Override
    public EntityModel<PracticanteResponse> toModel(Practicante practicante) {
        long id = practicante.getId();
        PracticanteController api = methodOn(PracticanteController.class);
        Set<EstadoPracticante> transiciones = practicante.getEstado().transicionesPermitidas();

        // self lleva la "affordance" de cambiar estado: en HAL-FORMS se publica como plantilla (_templates).
        // Siempre hay al menos una transición posible (ver EstadoPracticante.transicionesPermitidas).
        Link self = linkTo(api.obtener(id, null)).withSelfRel()
                .andAffordance(afford(api.actualizarEstado(id, null, null)));

        EntityModel<PracticanteResponse> modelo = EntityModel.of(PracticanteResponse.desde(practicante), self);
        modelo.add(linkTo(api.descargarHojaVida(id)).withRel(RelacionesPracticante.HOJA_VIDA)
                .withType(MediaType.APPLICATION_PDF_VALUE)
                .withTitle(practicante.getHojaVidaNombreOriginal()));
        modelo.add(linkTo(api.listar(null, null)).withRel(IanaLinkRelations.COLLECTION));

        for (EstadoPracticante destino : transiciones) {
            // name = valor que el cliente debe enviar en {"estado": ...} al seguir el enlace con PATCH.
            modelo.add(linkTo(api.actualizarEstado(id, null, null))
                    .withRel(RelacionesPracticante.marcar(destino))
                    .withName(destino.name())
                    .withTitle(RelacionesPracticante.titulo(destino)));
        }
        return modelo;
    }

    /** Enlace a la raíz de la API (punto de entrada), común a la colección y a cada practicante. */
    static Link raiz() {
        return Link.of(ServletUriComponentsBuilder.fromCurrentContextPath().path(Rutas.API).toUriString(), RelacionesPracticante.RAIZ);
    }
}
