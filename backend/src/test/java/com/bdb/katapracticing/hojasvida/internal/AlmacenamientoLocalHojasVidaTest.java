package com.bdb.katapracticing.hojasvida.internal;

import com.bdb.katapracticing.compartido.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlmacenamientoLocalHojasVidaTest {

    private static final byte[] CONTENIDO = "%PDF-1.4 prueba".getBytes(StandardCharsets.US_ASCII);

    @TempDir Path raiz;
    private Path carpeta;
    private AlmacenamientoLocalHojasVida almacenamiento;

    @BeforeEach
    void configurar() {
        carpeta = raiz.resolve("uploads");
        almacenamiento = new AlmacenamientoLocalHojasVida(carpeta.toString());
    }

    @Test
    void guardarYCargar_devuelveElMismoContenido() throws IOException {
        String id = almacenamiento.guardar(new ByteArrayInputStream(CONTENIDO));

        Resource recurso = almacenamiento.cargar(id);

        assertThat(id).endsWith(".pdf");
        assertThat(recurso.getContentAsByteArray()).isEqualTo(CONTENIDO);
    }

    @Test
    void eliminar_borraElArchivo() {
        String id = almacenamiento.guardar(new ByteArrayInputStream(CONTENIDO));

        almacenamiento.eliminar(id);

        assertThat(Files.exists(carpeta.resolve(id))).isFalse();
    }

    @Test
    void cargar_noPermiteSalirDeLaCarpeta() throws IOException {
        Files.writeString(raiz.resolve("secreto.txt"), "no");

        assertThatThrownBy(() -> almacenamiento.cargar("../secreto.txt"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatCode(() -> almacenamiento.eliminar("../secreto.txt")).doesNotThrowAnyException();
        assertThat(Files.exists(raiz.resolve("secreto.txt"))).isTrue();
    }

    @Test
    void cargar_archivoInexistente_lanzaNoEncontrado() {
        assertThatThrownBy(() -> almacenamiento.cargar("no-existe.pdf"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void constructor_siLaCarpetaNoSePuedeCrear_fallaAlArrancar() throws IOException {
        Path archivoQueBloquea = Files.writeString(raiz.resolve("no-es-carpeta"), "x");
        String carpetaImposible = archivoQueBloquea.resolve("uploads").toString();

        assertThatThrownBy(() -> new AlmacenamientoLocalHojasVida(carpetaImposible))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("No se pudo crear la carpeta");
    }

    @Test
    void guardar_siFallaLaLectura_lanzaErrorDeEntradaSalida() {
        InputStream rota = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("disco desconectado");
            }
        };

        assertThatThrownBy(() -> almacenamiento.guardar(rota))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessage("No se pudo guardar la hoja de vida");
    }

    @Test
    void cargar_identificadorVacio_noDevuelveLaCarpeta() {
        assertThatThrownBy(() -> almacenamiento.cargar(""))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
