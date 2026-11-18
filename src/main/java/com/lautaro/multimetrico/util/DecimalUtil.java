package com.lautaro.multimetrico.util;

import java.math.BigDecimal;

public final class DecimalUtil {

    private DecimalUtil() {
    }

    public static BigDecimal comoEntro(BigDecimal deLaBase) {
        if (deLaBase == null) {
            return null;
        }
        BigDecimal sinCeros = deLaBase.stripTrailingZeros();
        return sinCeros.scale() < 0 ? sinCeros.setScale(0) : sinCeros;
    }
}
