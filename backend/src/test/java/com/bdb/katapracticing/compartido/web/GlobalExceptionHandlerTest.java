package com.bdb.katapracticing.compartido.web;

import com.bdb.katapracticing.compartido.ErrorResponse;
import com.bdb.katapracticing.compartido.TipoError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler manejador = new GlobalExceptionHandler(DataSize.ofMegabytes(5));

    /**
     * MockMvc no aplica el límite de multipart, así que el error 413 se prueba directamente.
     * El mensaje sale de la configuración (spring.servlet.multipart.max-file-size), no de un texto fijo.
     */
    @Test
    void archivoMuyGrande_responde413ConElLimiteConfigurado() {
        ResponseEntity<ErrorResponse> respuesta = manejador.archivoMuyGrande();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().mensaje()).isEqualTo("El archivo no puede superar 5 MB");
    }

    @ParameterizedTest
    @CsvSource({"DATO_INVALIDO, BAD_REQUEST", "CONFLICTO, CONFLICT", "NO_ENCONTRADO, NOT_FOUND",
            "PRECONDICION_FALLIDA, PRECONDITION_FAILED"})
    void cadaTipoDeErrorTieneSuCodigoHttp(TipoError tipo, HttpStatus esperado) {
        assertThat(GlobalExceptionHandler.estadoHttp(tipo)).isEqualTo(esperado);
    }

    @Test
    void edicionSimultaneaEIntegridad_responden409() {
        assertThat(manejador.edicionSimultanea().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(manejador.integridadDeDatos().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @ParameterizedTest
    @CsvSource({"METHOD_NOT_ALLOWED, Método HTTP no permitido para este recurso",
            "UNSUPPORTED_MEDIA_TYPE, Tipo de contenido no soportado",
            "NOT_FOUND, El recurso solicitado no existe",
            "BAD_REQUEST, Solicitud inválida"})
    void erroresDeProtocolo_tienenMensajeEnEspanol(HttpStatus estado, String mensaje) {
        assertThat(GlobalExceptionHandler.mensajeDeProtocolo(estado)).isEqualTo(mensaje);
    }
}
