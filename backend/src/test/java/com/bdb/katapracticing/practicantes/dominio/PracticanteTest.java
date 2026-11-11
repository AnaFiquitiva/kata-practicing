package com.bdb.katapracticing.practicantes.dominio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @ParameterizedTest
    @ValueSource(ints = {0, 11, 12, -1})
    void registrar_rechazaSemestreFueraDeRango(int semestre) {
        assertThatThrownBy(() -> Practicante.registrar("Ana", "ana@correo.com", "Sistemas", semestre, "cv.pdf", "u.pdf"))
                .isInstanceOf(SemestreFueraDeRangoException.class)
                .hasMessage("El semestre debe estar entre 1 y 10");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10})
    void registrar_aceptaLosLimitesDelSemestre(int semestre) {
        assertThat(Practicante.registrar("Ana", "ana@correo.com", "Sistemas", semestre, "cv.pdf", "u.pdf").getSemestre())
                .isEqualTo(semestre);
    }

    @Test
    void cambiarEstado_alMismoEstado_esUnaTransicionInvalida() {
        Practicante practicante = nuevo();

        assertThatThrownBy(() -> practicante.cambiarEstado(EstadoPracticante.PENDIENTE))
                .isInstanceOf(TransicionDeEstadoInvalidaException.class);
        assertThat(practicante.getFechaActualizacionEstado()).isNull();
    }

    @Test
    void cadaEstadoPermiteIrALosOtrosDos() {
        assertThat(EstadoPracticante.PENDIENTE.transicionesPermitidas())
                .containsExactlyInAnyOrder(EstadoPracticante.VIABLE, EstadoPracticante.NO_VIABLE);
        assertThat(EstadoPracticante.VIABLE.puedeCambiarA(null)).isFalse();
    }

    @Test
    void tieneVersion_comparaConLaVersionActual() {
        Practicante practicante = nuevo();

        assertThat(practicante.tieneVersion(0)).isTrue();
        assertThat(practicante.tieneVersion(1)).isFalse();
    }
}
