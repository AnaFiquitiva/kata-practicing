package com.bdb.katapracticing.dto;

import com.bdb.katapracticing.model.EstadoPracticante;
import com.bdb.katapracticing.model.Practicante;

import java.time.LocalDateTime;

/** Lo que la API expone: nunca revela la ruta interna del archivo en el servidor. */
public record PracticanteResponse(
        Long id,
        String nombreCompleto,
        String correo,
        String carrera,
        Integer semestre,
        String hojaVidaNombre,
        String hojaVidaUrl,
        EstadoPracticante estado,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaActualizacionEstado
) {
    public static PracticanteResponse desde(Practicante p) {
        return new PracticanteResponse(
                p.getId(),
                p.getNombreCompleto(),
                p.getCorreo(),
                p.getCarrera(),
                p.getSemestre(),
                p.getHojaVidaNombreOriginal(),
                "/api/practicantes/" + p.getId() + "/hoja-vida",
                p.getEstado(),
                p.getFechaRegistro(),
                p.getFechaActualizacionEstado()
        );
    }
}
