package com.bdb.katapracticing.hojasvida;

import org.springframework.core.io.Resource;

/** Hoja de vida lista para entregarse al analista: nombre original + contenido. */
public record HojaVidaDescargable(String nombre, Resource contenido) {
}
