package com.bdb.katapracticing.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Respuestas de error uniformes en JSON: { estado, mensaje, errores?, fecha }. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> validacion(BindException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return cuerpo(HttpStatus.BAD_REQUEST, "Hay campos inválidos en el formulario", errores);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> archivoFaltante(MissingServletRequestPartException ex) {
        return cuerpo(HttpStatus.BAD_REQUEST, "La hoja de vida (PDF) es obligatoria",
                Map.of(ex.getRequestPartName(), "La hoja de vida (PDF) es obligatoria"));
    }

    @ExceptionHandler(ArchivoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> archivoInvalido(ArchivoInvalidoException ex) {
        return cuerpo(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of("hojaVida", ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> archivoMuyGrande(MaxUploadSizeExceededException ex) {
        return cuerpo(HttpStatus.PAYLOAD_TOO_LARGE, "La hoja de vida no puede superar 5 MB",
                Map.of("hojaVida", "La hoja de vida no puede superar 5 MB"));
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> duplicado(CorreoDuplicadoException ex) {
        return cuerpo(HttpStatus.CONFLICT, ex.getMessage(), Map.of("correo", ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex) {
        return cuerpo(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> cuerpoInvalido(HttpMessageNotReadableException ex) {
        return cuerpo(HttpStatus.BAD_REQUEST,
                "Solicitud inválida. Estados permitidos: PENDIENTE, VIABLE, NO_VIABLE", null);
    }

    private ResponseEntity<Map<String, Object>> cuerpo(HttpStatus status, String mensaje, Map<String, String> errores) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("estado", status.value());
        body.put("mensaje", mensaje);
        if (errores != null && !errores.isEmpty()) {
            body.put("errores", errores);
        }
        body.put("fecha", LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }
}
