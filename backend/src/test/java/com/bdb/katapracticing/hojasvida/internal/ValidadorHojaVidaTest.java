package com.bdb.katapracticing.hojasvida.internal;

import com.bdb.katapracticing.hojasvida.ArchivoInvalidoException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidadorHojaVidaTest {

    private static final byte[] PDF = "%PDF-1.7\n...".getBytes(StandardCharsets.US_ASCII);

    private final ValidadorHojaVida validador = new ValidadorHojaVida();

    private static MockMultipartFile archivo(String nombre, byte[] contenido) {
        return new MockMultipartFile("hojaVida", nombre, "application/pdf", contenido);
    }

    @Test
    void aceptaUnPdfReal() {
        assertThatCode(() -> validador.validar(archivo("CV.PDF", PDF))).doesNotThrowAnyException();
    }

    @Test
    void rechazaArchivoNuloOVacio() {
        assertThatThrownBy(() -> validador.validar(null))
                .isInstanceOf(ArchivoInvalidoException.class)
                .hasMessage(ValidadorHojaVida.MENSAJE_OBLIGATORIA);
        assertThatThrownBy(() -> validador.validar(archivo("cv.pdf", new byte[0])))
                .hasMessage(ValidadorHojaVida.MENSAJE_OBLIGATORIA);
    }

    @Test
    void rechazaExtensionDistintaDePdf() {
        assertThatThrownBy(() -> validador.validar(archivo("cv.docx", PDF)))
                .hasMessage(ValidadorHojaVida.MENSAJE_EXTENSION);
    }

    @Test
    void rechazaArchivoRenombradoQueNoEsPdf() {
        byte[] ejecutable = "MZ\u0090\u0000".getBytes(StandardCharsets.ISO_8859_1);
        assertThatThrownBy(() -> validador.validar(archivo("cv.pdf", ejecutable)))
                .hasMessage(ValidadorHojaVida.MENSAJE_NO_ES_PDF);
    }

    @Test
    void rechazaArchivoSinNombre() {
        MultipartFile sinNombre = mock(MultipartFile.class);
        when(sinNombre.isEmpty()).thenReturn(false);
        when(sinNombre.getOriginalFilename()).thenReturn(null);

        assertThatThrownBy(() -> validador.validar(sinNombre)).hasMessage(ValidadorHojaVida.MENSAJE_EXTENSION);
    }

    @Test
    void siNoSePuedeLeerElArchivo_loTrataComoNoPdf() throws IOException {
        MultipartFile ilegible = mock(MultipartFile.class);
        when(ilegible.isEmpty()).thenReturn(false);
        when(ilegible.getOriginalFilename()).thenReturn("cv.pdf");
        when(ilegible.getInputStream()).thenThrow(new IOException("subida interrumpida"));

        assertThatThrownBy(() -> validador.validar(ilegible)).hasMessage(ValidadorHojaVida.MENSAJE_NO_ES_PDF);
    }
}
