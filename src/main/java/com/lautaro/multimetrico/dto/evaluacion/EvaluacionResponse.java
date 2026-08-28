package com.lautaro.multimetrico.dto.evaluacion;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * La fila del multimétrico tal como la ve el asesor.
 *
 * <p>Cada métrica viaja con la banda en que cayó, no solo con el resultado. Es
 * la diferencia entre un número que hay que creer y uno que se puede discutir.
 */
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
