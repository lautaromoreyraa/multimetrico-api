package com.lautaro.multimetrico.dto.evaluacion;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public record Evaluacion(
        YearMonth periodo,
        String campania,
        Map<Metrica, Alcance> alcances,
        List<Metrica> faltantes,
        List<Metrica> fueraDeEscala,
        BigDecimal alcanceTotal,
        boolean completa) {
}
