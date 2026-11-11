package com.bdb.katapracticing.practicantes.web;

import com.bdb.katapracticing.practicantes.dominio.Practicante;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Solo documentación (OpenAPI): describe el formulario multipart completo del registro, con los
 * campos de texto y el PDF en el mismo cuerpo, tal como lo envía el front-end. Así Swagger UI
 * muestra un formulario con selector de archivo. En ejecución se usan
 * {@link RegistroPracticanteRequest} y el {@code MultipartFile}.
 */
@Schema(name = "FormularioRegistro", description = "Formulario de registro (multipart/form-data)",
        requiredProperties = {"nombreCompleto", "correo", "carrera", "semestre", "hojaVida"})
record FormularioRegistroDoc(
        @Schema(example = "Ana Pérez", maxLength = Practicante.LONGITUD_MAXIMA_TEXTO) String nombreCompleto,
        @Schema(example = "ana.perez@correo.com", format = "email") String correo,
        @Schema(example = "Ingeniería de Sistemas") String carrera,
        @Schema(example = "8", minimum = "" + Practicante.SEMESTRE_MINIMO, maximum = "" + Practicante.SEMESTRE_MAXIMO)
        Integer semestre,
        @Schema(type = "string", format = "binary", description = "Hoja de vida en PDF (máx. 5 MB)") String hojaVida
) {
}
