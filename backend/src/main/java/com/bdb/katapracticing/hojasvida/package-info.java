/**
 * Módulo <b>hojas de vida</b>: valida, guarda y entrega los PDF.
 * <p>
 * API pública: {@link com.bdb.katapracticing.hojasvida.HojasVidaService} y sus tipos de resultado.
 * Todo lo que está en {@code internal} (validador, almacenamiento en disco) es privado del módulo.
 */
@ApplicationModule(displayName = "Hojas de vida", allowedDependencies = "compartido")
package com.bdb.katapracticing.hojasvida;

import org.springframework.modulith.ApplicationModule;
