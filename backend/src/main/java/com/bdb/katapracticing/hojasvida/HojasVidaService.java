package com.bdb.katapracticing.hojasvida;

import com.bdb.katapracticing.hojasvida.internal.AlmacenamientoHojasVida;
import com.bdb.katapracticing.hojasvida.internal.NombresArchivo;
import com.bdb.katapracticing.hojasvida.internal.ValidadorHojaVida;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * API pública del módulo de hojas de vida (Facade). Los demás módulos solo conocen esta clase;
 * cómo se valida, cómo se nombra y dónde se guarda el archivo son detalles internos del módulo.
 */
@Service
public class HojasVidaService {

    private final ValidadorHojaVida validador;
    private final AlmacenamientoHojasVida almacenamiento;

    public HojasVidaService(ValidadorHojaVida validador, AlmacenamientoHojasVida almacenamiento) {
        this.validador = validador;
        this.almacenamiento = almacenamiento;
    }

    /**
     * Valida que el archivo sea un PDF real y lo guarda.
     *
     * @throws ArchivoInvalidoException si no es un PDF aceptable
     */
    public HojaVidaGuardada guardar(MultipartFile archivo) {
        validador.validar(archivo);
        try (InputStream contenido = archivo.getInputStream()) {
            String identificador = almacenamiento.guardar(contenido);
            return new HojaVidaGuardada(identificador, NombresArchivo.paraMostrar(archivo.getOriginalFilename()));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la hoja de vida", e);
        }
    }

    /** @throws com.bdb.katapracticing.compartido.RecursoNoEncontradoException si el archivo no existe. */
    public HojaVidaDescargable cargar(String identificador, String nombreParaMostrar) {
        return new HojaVidaDescargable(nombreParaMostrar, almacenamiento.cargar(identificador));
    }

    /** Eliminación de mejor esfuerzo: no falla si el archivo ya no existe. */
    public void eliminar(String identificador) {
        almacenamiento.eliminar(identificador);
    }
}
