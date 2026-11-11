package com.bdb.katapracticing.hojasvida;

/**
 * Resultado de guardar una hoja de vida.
 *
 * @param identificador     nombre interno con el que se recupera (nunca se expone en la API)
 * @param nombreParaMostrar nombre original ya limpio, para mostrar y descargar
 */
public record HojaVidaGuardada(String identificador, String nombreParaMostrar) {
}
