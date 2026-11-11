package com.bdb.katapracticing.practicantes.dominio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class PracticanteTest {

    private static Practicante nuevo() {
        return Practicante.registrar("  Ana Pérez ", " Ana@Correo.COM ", " Sistemas ", 8, "cv.pdf", "uuid.pdf");
    }

    @Test
    void registrar_normalizaDatosYNaceEnPendiente() {
        Practicante practicante = nuevo();

        assertThat(practicante.getNombreCompleto()).isEqualTo("Ana Pérez");
        assertThat(practicante.getCorreo()).isEqualTo("ana@correo.com");
        assertThat(practicante.getCarrera()).isEqualTo("Sistemas");
        assertThat(practicante.getEstado()).isEqualTo(EstadoPracticante.PENDIENTE);
        assertThat(practicante.getFechaActualizacionEstado()).isNull();
    }

    @Test
    void cambiarEstado_registraLaFechaDelCambio() {
        Practicante practicante = nuevo();

        practicante.cambiarEstado(EstadoPracticante.VIABLE);

        assertThat(practicante.getEstado()).isEqualTo(EstadoPracticante.VIABLE);
        assertThat(practicante.getFechaActualizacionEstado()).isNotNull();
    }

    @Test
    void cambiarEstado_rechazaNulo() {
        Practicante practicante = nuevo();

        assertThatNullPointerException().isThrownBy(() -> practicante.cambiarEstado(null));
        assertThat(practicante.getEstado()).isEqualTo(EstadoPracticante.PENDIENTE);
    }
}
