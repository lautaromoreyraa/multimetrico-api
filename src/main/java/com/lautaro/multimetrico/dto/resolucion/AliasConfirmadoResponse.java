package com.lautaro.multimetrico.dto.resolucion;

import java.util.List;

/**
 * El acuse de un alias confirmado.
 *
 * <p>Devuelve todos los alias que ese legajo acumuló, no solo el que se acaba de
 * confirmar. Es lo que permite darse cuenta de que una persona junta cinco formas
 * distintas de escribir su nombre —o, peor, de que un alias quedó apuntando a
 * quien no era y hay que corregirlo—.
 */
public record AliasConfirmadoResponse(
        String texto,
        String legajo,
        String nombre,
        List<String> aliasDeEsteLegajo) {
}
