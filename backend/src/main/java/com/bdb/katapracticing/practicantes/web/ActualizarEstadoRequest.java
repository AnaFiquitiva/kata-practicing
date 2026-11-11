package com.bdb.katapracticing.practicantes.web;

import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPracticante estado
) {
}
