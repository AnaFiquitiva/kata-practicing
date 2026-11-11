package com.bdb.katapracticing.practicantes.web;

import com.bdb.katapracticing.practicantes.aplicacion.PracticanteService.DatosRegistro;
import com.bdb.katapracticing.practicantes.dominio.Practicante;
import com.bdb.katapracticing.practicantes.dominio.SemestreFueraDeRangoException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Campos de texto del formulario de registro (la hoja de vida llega como MultipartFile aparte). */
@Schema(description = "Datos del formulario de registro del practicante")
public record RegistroPracticanteRequest(

        @Schema(description = "Nombre completo", example = "Ana Pérez", maxLength = Practicante.LONGITUD_MAXIMA_TEXTO)
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = Practicante.LONGITUD_MAXIMA_TEXTO, message = "El nombre no puede superar 120 caracteres")
        String nombreCompleto,

        @Schema(description = "Correo electrónico (único; se guarda en minúsculas)", example = "ana.perez@correo.com")
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no es válido")
        @Size(max = Practicante.LONGITUD_MAXIMA_TEXTO, message = "El correo no puede superar 120 caracteres")
        String correo,

        @Schema(description = "Carrera universitaria", example = "Ingeniería de Sistemas")
        @NotBlank(message = "La carrera universitaria es obligatoria")
        @Size(max = Practicante.LONGITUD_MAXIMA_TEXTO, message = "La carrera no puede superar 120 caracteres")
        String carrera,

        @Schema(description = "Semestre actual", example = "8",
                minimum = "" + Practicante.SEMESTRE_MINIMO, maximum = "" + Practicante.SEMESTRE_MAXIMO)
        @NotNull(message = "El semestre es obligatorio")
        @Min(value = Practicante.SEMESTRE_MINIMO, message = SemestreFueraDeRangoException.MENSAJE)
        @Max(value = Practicante.SEMESTRE_MAXIMO, message = SemestreFueraDeRangoException.MENSAJE)
        Integer semestre
) {
    public DatosRegistro comoDatosRegistro() {
        return new DatosRegistro(nombreCompleto, correo, carrera, semestre);
    }
}
