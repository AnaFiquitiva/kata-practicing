package com.bdb.katapracticing.compartido.web;

import com.bdb.katapracticing.compartido.ErrorDeNegocio;
import com.bdb.katapracticing.compartido.ErrorResponse;
import com.bdb.katapracticing.compartido.TipoError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Errores comunes a toda la API, en el formato único {@link ErrorResponse}.
 * Los errores propios de un módulo los traduce el propio módulo (por ejemplo, practicantes.web).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String MENSAJE_FORMULARIO_INVALIDO = "Hay campos inválidos en el formulario";

    private final String mensajeArchivoGrande;

    public GlobalExceptionHandler(@Value("${spring.servlet.multipart.max-file-size}") DataSize tamanoMaximo) {
        this.mensajeArchivoGrande = "El archivo no puede superar " + tamanoMaximo.toMegabytes() + " MB";
    }

    @ExceptionHandler(ErrorDeNegocio.class)
    public ResponseEntity<ErrorResponse> errorDeNegocio(ErrorDeNegocio ex) {
        HttpStatus estado = estadoHttp(ex.getTipo());
        ErrorResponse error = ex.getCampo()
                .map(campo -> ErrorResponse.deCampo(estado, campo, ex.getMessage()))
                .orElseGet(() -> ErrorResponse.de(estado, ex.getMessage()));
        return responder(error);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> validacion(BindException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return responder(ErrorResponse.de(HttpStatus.BAD_REQUEST, MENSAJE_FORMULARIO_INVALIDO, errores));
    }

    /** Se lanza antes de saber qué controlador atiende la petición, por eso vive aquí y no en un módulo. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> archivoMuyGrande() {
        return responder(ErrorResponse.de(HttpStatus.PAYLOAD_TOO_LARGE, mensajeArchivoGrande));
    }

    static HttpStatus estadoHttp(TipoError tipo) {
        return switch (tipo) {
            case DATO_INVALIDO -> HttpStatus.BAD_REQUEST;
            case CONFLICTO -> HttpStatus.CONFLICT;
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
        };
    }

    private static ResponseEntity<ErrorResponse> responder(ErrorResponse error) {
        return ResponseEntity.status(error.estado()).body(error);
    }
}
