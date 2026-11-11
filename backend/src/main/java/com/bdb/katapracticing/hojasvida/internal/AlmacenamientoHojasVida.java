package com.bdb.katapracticing.hojasvida.internal;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * Puerto de almacenamiento de hojas de vida. El servicio depende de esta abstracción
 * y no de "dónde" se guardan los archivos: hoy es una carpeta local
 * ({@link AlmacenamientoLocalHojasVida}); mañana podría ser S3 o Azure Blob sin tocar
 * la lógica de negocio.
 */
public interface AlmacenamientoHojasVida {

    /** Guarda el contenido y devuelve el identificador interno con el que se recupera. */
    String guardar(InputStream contenido);

    /** @throws com.bdb.katapracticing.compartido.RecursoNoEncontradoException si no existe. */
    Resource cargar(String identificador);

    /** Eliminación de mejor esfuerzo (por ejemplo, para no dejar archivos huérfanos). */
    void eliminar(String identificador);
}
