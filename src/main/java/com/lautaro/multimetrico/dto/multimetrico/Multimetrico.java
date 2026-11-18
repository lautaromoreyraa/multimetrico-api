package com.lautaro.multimetrico.dto.multimetrico;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record Multimetrico(
        YearMonth periodo,
        String campania,
        String lider,
        BigDecimal alcanceMaximo,
        ResumenDeEquipo resumen,
        List<FilaDeMultimetrico> filas) {

    public Multimetrico {
        filas = List.copyOf(filas);
    }
}
