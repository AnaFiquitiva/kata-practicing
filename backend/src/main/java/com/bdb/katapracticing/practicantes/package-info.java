/**
 * Módulo <b>practicantes</b>: registro del practicante y decisión del analista (Viable / No viable).
 * <p>
 * Es el único dueño de la entidad {@code Practicante} y de su tabla. Por dentro mantiene capas
 * ({@code web}, {@code aplicacion}, {@code dominio}, {@code persistencia}); ninguna es visible desde otros módulos.
 * Usa el módulo de hojas de vida solo a través de su API pública ({@code HojasVidaService}).
 */
@ApplicationModule(displayName = "Practicantes", allowedDependencies = {"compartido", "hojasvida"})
package com.bdb.katapracticing.practicantes;

import org.springframework.modulith.ApplicationModule;
