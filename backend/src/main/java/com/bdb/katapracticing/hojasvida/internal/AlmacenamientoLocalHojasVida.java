package com.bdb.katapracticing.hojasvida.internal;

import com.bdb.katapracticing.compartido.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Guarda las hojas de vida en una carpeta del servidor, con nombre UUID (sin colisiones ni path traversal). */
@Component
public class AlmacenamientoLocalHojasVida implements AlmacenamientoHojasVida {

    private static final Logger LOG = LoggerFactory.getLogger(AlmacenamientoLocalHojasVida.class);
    private static final String EXTENSION = ".pdf";
    private static final String NO_ENCONTRADA = "La hoja de vida no se encuentra en el servidor";

    private final Path directorio;

    public AlmacenamientoLocalHojasVida(@Value("${app.uploads.dir}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.directorio);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de archivos: " + this.directorio, e);
        }
    }

    @Override
    public String guardar(InputStream contenido) {
        String identificador = UUID.randomUUID() + EXTENSION;
        try {
            Files.copy(contenido, directorio.resolve(identificador));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la hoja de vida", e);
        }
        return identificador;
    }

    @Override
    public Resource cargar(String identificador) {
        Path ruta = resolverDentroDelDirectorio(identificador);
        if (!Files.isReadable(ruta)) {
            throw new RecursoNoEncontradoException(NO_ENCONTRADA);
        }
        return new PathResource(ruta);
    }

    @Override
    public void eliminar(String identificador) {
        try {
            Files.deleteIfExists(resolverDentroDelDirectorio(identificador));
        } catch (IOException | RecursoNoEncontradoException e) {
            LOG.warn("No se pudo eliminar la hoja de vida {}", identificador, e);
        }
    }

    /** Impide que un identificador manipulado ("../../etc") salga de la carpeta de archivos. */
    private Path resolverDentroDelDirectorio(String identificador) {
        Path ruta = directorio.resolve(identificador).normalize();
        if (!ruta.startsWith(directorio) || ruta.equals(directorio)) {
            throw new RecursoNoEncontradoException(NO_ENCONTRADA);
        }
        return ruta;
    }
}
