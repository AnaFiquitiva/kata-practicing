package com.bdb.katapracticing.compartido.web;

import com.bdb.katapracticing.compartido.ErrorDeNegocio;
import com.bdb.katapracticing.compartido.ErrorResponse;
import com.bdb.katapracticing.compartido.PrecondicionFallidaException;
import com.bdb.katapracticing.compartido.TipoError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
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
    private static final String MENSAJE_INTEGRIDAD = "La operación viola una regla de integridad de los datos";

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

    /** Un parámetro de la URL no tiene el tipo esperado (por ejemplo, /practicantes/abc o ?estado=QUIZAS). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroConTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(ErrorResponse.deCampo(HttpStatus.BAD_REQUEST, ex.getName(),
                "El valor '" + ex.getValue() + "' no es válido para '" + ex.getName() + "'"));
    }

    /** Otra transacción modificó el registro entre la lectura y la escritura (bloqueo optimista con @Version). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> edicionSimultanea() {
        return responder(ErrorResponse.de(HttpStatus.CONFLICT, PrecondicionFallidaException.MENSAJE));
    }

    /** Última línea de defensa: una restricción de la BD (UNIQUE, NOT NULL, CHECK) rechazó los datos. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridadDeDatos() {
        return responder(ErrorResponse.de(HttpStatus.CONFLICT, MENSAJE_INTEGRIDAD));
    }

    /** Errores del protocolo HTTP (405, 415, 404 de ruta, parámetro obligatorio) en el formato único de la API. */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class,
            NoResourceFoundException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> errorDeProtocolo(Exception ex) {
        org.springframework.web.ErrorResponse detalle = (org.springframework.web.ErrorResponse) ex;
        HttpStatus estado = HttpStatus.valueOf(detalle.getStatusCode().value());
        // Conserva cabeceras útiles como Allow (405) o Accept (415).
        return ResponseEntity.status(estado).headers(detalle.getHeaders())
                .body(ErrorResponse.de(estado, mensajeDeProtocolo(estado)));
    }

    static String mensajeDeProtocolo(HttpStatus estado) {
        return switch (estado) {
            case METHOD_NOT_ALLOWED -> "Método HTTP no permitido para este recurso";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de contenido no soportado";
            case NOT_FOUND -> "El recurso solicitado no existe";
            default -> "Solicitud inválida";
        };
    }

    static HttpStatus estadoHttp(TipoError tipo) {
        return switch (tipo) {
            case DATO_INVALIDO -> HttpStatus.BAD_REQUEST;
            case CONFLICTO -> HttpStatus.CONFLICT;
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case PRECONDICION_FALLIDA -> HttpStatus.PRECONDITION_FAILED;
        };
    }

    private static ResponseEntity<ErrorResponse> responder(ErrorResponse error) {
        return ResponseEntity.status(error.estado()).body(error);
    }
}
