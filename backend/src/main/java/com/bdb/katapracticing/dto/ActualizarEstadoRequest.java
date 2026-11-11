package com.bdb.katapracticing.dto;

import com.bdb.katapracticing.model.EstadoPracticante;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPracticante estado
) {
}
