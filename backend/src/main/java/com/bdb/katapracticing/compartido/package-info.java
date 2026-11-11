/**
 * Módulo <b>compartido</b>: contrato común de la API (rutas, formato de error y errores de negocio base).
 * No depende de ningún otro módulo; todos los demás pueden usarlo.
 */
@ApplicationModule(displayName = "Compartido", allowedDependencies = {})
package com.bdb.katapracticing.compartido;

import org.springframework.modulith.ApplicationModule;
