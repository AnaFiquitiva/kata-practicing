/**
 * Módulo <b>seguridad</b>: autenticación del analista (login/logout por API, sesión, CSRF)
 * y reglas de acceso a cada ruta. Solo conoce las rutas y el formato de error del módulo compartido.
 */
@ApplicationModule(displayName = "Seguridad", allowedDependencies = "compartido")
package com.bdb.katapracticing.seguridad;

import org.springframework.modulith.ApplicationModule;
