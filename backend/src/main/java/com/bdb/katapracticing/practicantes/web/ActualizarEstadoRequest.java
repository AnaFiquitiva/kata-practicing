package com.bdb.katapracticing.practicantes.web;

import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo del PATCH de estado. Es una clase con setter (no un record) porque HAL-FORMS
 * publica como {@code readOnly} las propiedades sin setter, y esta el cliente sí debe enviarla.
 */
@Schema(description = "Nuevo estado del practicante (debe ser distinto del actual)")
public class ActualizarEstadoRequest {

    @Schema(example = "VIABLE")
    @NotNull(message = "El estado es obligatorio")
    private EstadoPracticante estado;

    public EstadoPracticante getEstado() {
        return estado;
    }

    public void setEstado(EstadoPracticante estado) {
        this.estado = estado;
    }
}
