package com.bdb.katapracticing.hojasvida.internal;

import com.bdb.katapracticing.hojasvida.ArchivoInvalidoException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/** Reglas que debe cumplir una hoja de vida antes de guardarse. El tamaño máximo lo limita Spring (multipart). */
@Component
public class ValidadorHojaVida {

    static final String MENSAJE_OBLIGATORIA = "La hoja de vida (PDF) es obligatoria";
    static final String MENSAJE_EXTENSION = "La hoja de vida debe ser un archivo .pdf";
    static final String MENSAJE_NO_ES_PDF = "El archivo no es un PDF válido";

    private static final String EXTENSION_PDF = ".pdf";
    /** "Magic bytes": todo PDF inicia con %PDF-, sin importar la extensión del archivo. */
    private static final byte[] FIRMA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    public void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException(MENSAJE_OBLIGATORIA);
        }
        if (!tieneExtensionPdf(archivo.getOriginalFilename())) {
            throw new ArchivoInvalidoException(MENSAJE_EXTENSION);
        }
        if (!tieneFirmaPdf(archivo)) {
            throw new ArchivoInvalidoException(MENSAJE_NO_ES_PDF);
        }
    }

    private static boolean tieneExtensionPdf(String nombre) {
        return nombre != null && nombre.toLowerCase(Locale.ROOT).endsWith(EXTENSION_PDF);
    }

    private static boolean tieneFirmaPdf(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream()) {
            return Arrays.equals(entrada.readNBytes(FIRMA_PDF.length), FIRMA_PDF);
        } catch (IOException e) {
            return false;
        }
    }
}
