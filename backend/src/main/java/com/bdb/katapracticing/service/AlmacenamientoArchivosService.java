package com.bdb.katapracticing.service;

import com.bdb.katapracticing.exception.ArchivoInvalidoException;
import com.bdb.katapracticing.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

/** Guarda las hojas de vida en una carpeta local del servidor. */
@Service
public class AlmacenamientoArchivosService {

    private static final byte[] FIRMA_PDF = {'%', 'P', 'D', 'F', '-'};

    private final Path directorio;

    public AlmacenamientoArchivosService(@Value("${app.uploads.dir}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.directorio);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de archivos: " + this.directorio, e);
        }
    }

    /** Valida que sea un PDF real y lo guarda con un nombre UUID. Devuelve el nombre interno. */
    public String guardarPdf(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("La hoja de vida (PDF) es obligatoria");
        }
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase();
        if (!nombre.endsWith(".pdf")) {
            throw new ArchivoInvalidoException("La hoja de vida debe ser un archivo .pdf");
        }
        if (!tieneFirmaPdf(archivo)) {
            throw new ArchivoInvalidoException("El archivo no es un PDF válido");
        }

        String nombreInterno = UUID.randomUUID() + ".pdf";
        try (InputStream in = archivo.getInputStream()) {
            Files.copy(in, directorio.resolve(nombreInterno), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la hoja de vida", e);
        }
        return nombreInterno;
    }

    public Resource cargar(String nombreInterno) {
        Path ruta = directorio.resolve(nombreInterno).normalize();
        if (!ruta.startsWith(directorio) || !Files.isReadable(ruta)) {
            throw new RecursoNoEncontradoException("La hoja de vida no se encuentra en el servidor");
        }
        try {
            return new UrlResource(ruta.toUri());
        } catch (MalformedURLException e) {
            throw new RecursoNoEncontradoException("La hoja de vida no se encuentra en el servidor");
        }
    }

    public void eliminarSilenciosamente(String nombreInterno) {
        try {
            Files.deleteIfExists(directorio.resolve(nombreInterno));
        } catch (IOException ignored) {
            // Limpieza best-effort tras un fallo al guardar en BD.
        }
    }

    /** Revisa los "magic bytes": un PDF siempre inicia con %PDF-, sin importar la extensión. */
    private boolean tieneFirmaPdf(MultipartFile archivo) {
        try (InputStream in = archivo.getInputStream()) {
            byte[] cabecera = in.readNBytes(FIRMA_PDF.length);
            return Arrays.equals(cabecera, FIRMA_PDF);
        } catch (IOException e) {
            return false;
        }
    }
}
