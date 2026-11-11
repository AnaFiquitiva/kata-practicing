package com.bdb.katapracticing.practicantes.web;

import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import com.bdb.katapracticing.practicantes.dominio.Practicante;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;

/**
 * Lo que la API expone de un practicante: nunca revela el identificador interno del archivo en el servidor.
 * Las acciones posibles (descargar, cambiar estado…) no van como datos sino como enlaces {@code _links}.
 */
@Relation(itemRelation = "practicante", collectionRelation = "practicantes")
@Schema(description = "Practicante postulado")
public record PracticanteResponse(
        Long id,
        String nombreCompleto,
        String correo,
        String carrera,
        Integer semestre,
        @Schema(description = "Nombre original del PDF (para mostrar)") String hojaVidaNombre,
        EstadoPracticante estado,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaActualizacionEstado,
        @Schema(description = "Versión del registro; es la misma que la cabecera ETag. Envíala en If-Match al modificarlo")
        long version
) {
    public static PracticanteResponse desde(Practicante practicante) {
        return new PracticanteResponse(
                practicante.getId(),
                practicante.getNombreCompleto(),
                practicante.getCorreo(),
                practicante.getCarrera(),
                practicante.getSemestre(),
                practicante.getHojaVidaNombreOriginal(),
                practicante.getEstado(),
                practicante.getFechaRegistro(),
                practicante.getFechaActualizacionEstado(),
                practicante.getVersion()
        );
    }
}
