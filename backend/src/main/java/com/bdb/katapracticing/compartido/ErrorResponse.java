package com.bdb.katapracticing.compartido;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de error de la API: { estado, mensaje, errores?, fecha }.
 * Lo usan tanto el manejador global de excepciones como Spring Security (login fallido).
 */
public record ErrorResponse(
        int estado,
        String mensaje,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> errores,
        LocalDateTime fecha
) {
    public static ErrorResponse de(HttpStatus estado, String mensaje) {
        return de(estado, mensaje, Map.of());
    }

    public static ErrorResponse de(HttpStatus estado, String mensaje, Map<String, String> errores) {
        return new ErrorResponse(estado.value(), mensaje, Map.copyOf(errores), LocalDateTime.now());
    }

    /** Error asociado a un único campo del formulario. */
    public static ErrorResponse deCampo(HttpStatus estado, String campo, String mensaje) {
        return de(estado, mensaje, Map.of(campo, mensaje));
    }
}
