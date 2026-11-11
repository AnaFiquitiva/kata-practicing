package com.bdb.katapracticing.hojasvida.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NombresArchivoTest {

    @ParameterizedTest
    @CsvSource({
            "'C:\\fakepath\\mi cv.pdf', 'mi cv.pdf'",
            "'/home/ana/cv.pdf',        'cv.pdf'",
            "'hoja \"vida\".pdf',       'hoja vida.pdf'"
    })
    void quitaRutasYCaracteresPeligrosos(String original, String esperado) {
        assertThat(NombresArchivo.paraMostrar(original)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\"\""})
    void usaNombrePorDefectoSiNoQuedaNada(String original) {
        assertThat(NombresArchivo.paraMostrar(original)).isEqualTo(NombresArchivo.NOMBRE_POR_DEFECTO);
    }

    @Test
    void recortaNombresLargosConservandoLaExtension() {
        String largo = "a".repeat(300) + ".pdf";

        String resultado = NombresArchivo.paraMostrar(largo);

        assertThat(resultado).hasSize(NombresArchivo.LONGITUD_MAXIMA).endsWith(".pdf");
    }
}
