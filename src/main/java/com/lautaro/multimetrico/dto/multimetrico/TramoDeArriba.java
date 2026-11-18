package com.lautaro.multimetrico.dto.multimetrico;

import com.lautaro.multimetrico.enumeration.CondicionDeTramo;
import com.lautaro.multimetrico.enumeration.Sentido;
import java.math.BigDecimal;

public record TramoDeArriba(
        String banda,
        BigDecimal objetivo,
        BigDecimal distancia,
        CondicionDeTramo condicion,
        BigDecimal resultado,
        BigDecimal mejora) {
}
