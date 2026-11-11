package com.bdb.katapracticing;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Prueba de arquitectura del monolito modular. Falla si:
 * <ul>
 *   <li>un módulo usa una clase interna (subpaquete) de otro módulo,</li>
 *   <li>hay dependencias en ciclo entre módulos, o</li>
 *   <li>un módulo depende de otro que no declaró en {@code allowedDependencies}.</li>
 * </ul>
 */
class ModulosTest {

    private final ApplicationModules modulos = ApplicationModules.of(PracticantesApplication.class);

    @Test
    void lasFronterasEntreModulosSeRespetan() {
        assertThatCode(modulos::verify).doesNotThrowAnyException();
    }

    @Test
    void existenLosCuatroModulosEsperados() {
        assertThat(modulos.stream().map(ApplicationModule::getName))
                .containsExactlyInAnyOrder("practicantes", "hojasvida", "seguridad", "compartido");
    }

    /** Genera los diagramas de módulos (PlantUML) en target/spring-modulith-docs. */
    @Test
    void generarDocumentacionDeModulos() {
        new Documenter(modulos)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();

        assertThat(Path.of("target", "spring-modulith-docs", "components.puml")).exists();
    }
}
