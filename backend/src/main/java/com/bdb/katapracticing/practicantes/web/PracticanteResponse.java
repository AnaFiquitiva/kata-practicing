package com.bdb.katapracticing.practicantes.web;

import com.bdb.katapracticing.compartido.Rutas;
import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import com.bdb.katapracticing.practicantes.dominio.Practicante;

import java.time.LocalDateTime;

/** Lo que la API expone: nunca revela el identificador interno del archivo en el servidor. */
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
    public static PracticanteResponse desde(Practicante practicante) {
        return new PracticanteResponse(
                practicante.getId(),
                practicante.getNombreCompleto(),
                practicante.getCorreo(),
                practicante.getCarrera(),
                practicante.getSemestre(),
                practicante.getHojaVidaNombreOriginal(),
                Rutas.hojaVida(practicante.getId()),
                practicante.getEstado(),
                practicante.getFechaRegistro(),
                practicante.getFechaActualizacionEstado()
        );
    }
}
