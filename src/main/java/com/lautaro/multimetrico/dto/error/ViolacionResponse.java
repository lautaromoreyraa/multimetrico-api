package com.lautaro.multimetrico.dto.error;

import com.lautaro.multimetrico.dto.pliego.Violacion;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.TipoDeViolacion;

/**
 * Una violación del pliego con sus partes separadas, adentro del cuerpo de error.
 *
 * <p>Es la misma forma que {@link Violacion} y podría reusarse tal cual. Se
 * duplica a propósito: {@code Violacion} es también el cuerpo del {@code 200} de
 * la revisión, y atarlo al formato de error significaría que un cambio pensado
 * para el 422 sale por un endpoint que devuelve 200.
 */
public record ViolacionResponse(Metrica metrica, TipoDeViolacion tipo, String mensaje) {

    public static ViolacionResponse de(Violacion violacion) {
        return new ViolacionResponse(violacion.metrica(), violacion.tipo(), violacion.mensaje());
    }
}
