package com.lautaro.multimetrico.pliego.web;

import com.lautaro.multimetrico.pliego.domain.Alcance;
import com.lautaro.multimetrico.pliego.domain.Evaluacion;
import com.lautaro.multimetrico.pliego.domain.Metrica;
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

    public static EvaluacionResponse de(Evaluacion evaluacion) {
        List<AlcanceResponse> alcances = evaluacion.alcances().values().stream()
                .map(EvaluacionResponse::alcance)
                .sorted(java.util.Comparator.comparing(a -> a.metrica().name()))
                .toList();

        return new EvaluacionResponse(
                evaluacion.periodo(),
                evaluacion.campania(),
                evaluacion.alcanceTotal(),
                evaluacion.completa(),
                alcances,
                evaluacion.faltantes(),
                evaluacion.fueraDeEscala());
    }

    private static AlcanceResponse alcance(Alcance alcance) {
        return new AlcanceResponse(
                alcance.metrica(),
                alcance.valor(),
                alcance.banda().rango(),
                alcance.resultado());
    }
}
