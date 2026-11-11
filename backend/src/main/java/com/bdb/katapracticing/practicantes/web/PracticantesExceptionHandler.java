package com.bdb.katapracticing.practicantes.web;

import com.bdb.katapracticing.compartido.ErrorResponse;
import com.bdb.katapracticing.hojasvida.ArchivoInvalidoException;
import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Errores que solo tienen sentido en los endpoints de practicantes. Viven en este módulo
 * para que el manejador global (módulo compartido) no tenga que conocer los estados ni el formulario.
 */
@RestControllerAdvice(assignableTypes = PracticanteController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PracticantesExceptionHandler {

    private static final String MENSAJE_HOJA_VIDA_OBLIGATORIA = "La hoja de vida (PDF) es obligatoria";
    private static final String MENSAJE_ESTADO_INVALIDO = "Solicitud inválida. Estados permitidos: "
            + Arrays.stream(EstadoPracticante.values()).map(Enum::name).collect(Collectors.joining(", "));

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> hojaVidaFaltante() {
        return responder(ErrorResponse.deCampo(HttpStatus.BAD_REQUEST,
                ArchivoInvalidoException.CAMPO, MENSAJE_HOJA_VIDA_OBLIGATORIA));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> estadoIlegible() {
        return responder(ErrorResponse.de(HttpStatus.BAD_REQUEST, MENSAJE_ESTADO_INVALIDO));
    }

    private static ResponseEntity<ErrorResponse> responder(ErrorResponse error) {
        return ResponseEntity.status(error.estado()).body(error);
    }
}
