package com.lautaro.multimetrico.dto.evaluacion;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record EvaluacionResponse(
        YearMonth periodo,
        String campania,
        BigDecimal alcanceTotal,
        boolean completa,
        List<AlcanceResponse> alcances,
        List<Metrica> faltantes,
        List<Metrica> fueraDeEscala) {

    public record AlcanceResponse(
            Metrica metrica,
            BigDecimal valor,
            String banda,
            BigDecimal resultado) {
    }
}
