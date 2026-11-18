package com.lautaro.multimetrico.dto.error;

import com.lautaro.multimetrico.dto.pliego.Violacion;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.TipoDeViolacion;

public record ViolacionResponse(Metrica metrica, TipoDeViolacion tipo, String mensaje) {

    public static ViolacionResponse de(Violacion violacion) {
        return new ViolacionResponse(violacion.metrica(), violacion.tipo(), violacion.mensaje());
    }
}
