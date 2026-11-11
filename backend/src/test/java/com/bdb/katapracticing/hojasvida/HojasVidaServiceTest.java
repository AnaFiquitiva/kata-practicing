package com.bdb.katapracticing.hojasvida;

import com.bdb.katapracticing.hojasvida.internal.AlmacenamientoLocalHojasVida;
import com.bdb.katapracticing.hojasvida.internal.ValidadorHojaVida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Prueba el módulo de hojas de vida completo a través de su API pública, con una carpeta temporal. */
class HojasVidaServiceTest {

    private static final byte[] PDF = "%PDF-1.4 prueba".getBytes(StandardCharsets.US_ASCII);

    @TempDir Path carpeta;
    private HojasVidaService hojasVida;

    @BeforeEach
    void configurar() {
        hojasVida = new HojasVidaService(new ValidadorHojaVida(), new AlmacenamientoLocalHojasVida(carpeta.toString()));
    }

    @Test
    void guardarYCargar_conservaContenidoYNombreLimpio() throws IOException {
        var archivo = new MockMultipartFile("hojaVida", "C:\\fakepath\\mi cv.pdf", "application/pdf", PDF);

        HojaVidaGuardada guardada = hojasVida.guardar(archivo);
        HojaVidaDescargable descargable = hojasVida.cargar(guardada.identificador(), guardada.nombreParaMostrar());

        assertThat(guardada.nombreParaMostrar()).isEqualTo("mi cv.pdf");
        assertThat(descargable.contenido().getContentAsByteArray()).isEqualTo(PDF);
    }

    @Test
    void guardar_archivoInvalido_noEscribeNadaEnDisco() throws IOException {
        var falso = new MockMultipartFile("hojaVida", "cv.pdf", "application/pdf", "MZ".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> hojasVida.guardar(falso)).isInstanceOf(ArchivoInvalidoException.class);
        try (var archivos = Files.list(carpeta)) {
            assertThat(archivos).isEmpty();
        }
    }

    @Test
    void eliminar_borraElArchivo() {
        HojaVidaGuardada guardada = hojasVida.guardar(new MockMultipartFile("hojaVida", "cv.pdf", "application/pdf", PDF));

        hojasVida.eliminar(guardada.identificador());

        assertThat(carpeta.resolve(guardada.identificador())).doesNotExist();
    }

    /** El archivo pasó la validación pero falla al releerlo para guardarlo (por ejemplo, subida cortada). */
    @Test
    void guardar_siElArchivoNoSePuedeReleer_lanzaErrorDeEntradaSalida() throws IOException {
        ValidadorHojaVida validadorQueAcepta = mock(ValidadorHojaVida.class);
        HojasVidaService conValidadorQueAcepta =
                new HojasVidaService(validadorQueAcepta, new AlmacenamientoLocalHojasVida(carpeta.toString()));
        MultipartFile ilegible = mock(MultipartFile.class);
        when(ilegible.getInputStream()).thenThrow(new IOException("subida interrumpida"));

        assertThatThrownBy(() -> conValidadorQueAcepta.guardar(ilegible))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessage("No se pudo leer la hoja de vida");
    }
}
