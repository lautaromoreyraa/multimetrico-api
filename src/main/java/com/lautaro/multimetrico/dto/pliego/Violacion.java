package com.lautaro.multimetrico.dto.pliego;

import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.TipoDeViolacion;

public record Violacion(Metrica metrica, TipoDeViolacion tipo, String mensaje) {

    @Override
    public String toString() {
        return metrica == null ? mensaje : "%s: %s".formatted(metrica, mensaje);
    }
}
