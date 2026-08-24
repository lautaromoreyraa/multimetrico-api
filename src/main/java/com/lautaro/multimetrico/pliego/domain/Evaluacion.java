package com.lautaro.multimetrico.pliego.domain;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * La fila del multimétrico de un asesor: qué alcance le dio cada métrica y
 * cuánto suma en total.
 *
 * <p>Los faltantes y los que quedaron fuera de escala viajan aparte del total,
 * y no se los suma como cero. Un total de 9% con dos métricas pendientes no es
 * lo mismo que un 9% con todo cargado, y el que lo mira tiene que poder
 * distinguirlo.
 */
public record Evaluacion(
        YearMonth periodo,
        String campania,
        Map<Metrica, Alcance> alcances,
        List<Metrica> faltantes,
        List<Metrica> fueraDeEscala) {

    /** La suma de lo que efectivamente se pudo calcular. */
    public BigDecimal alcanceTotal() {
        return alcances.values().stream()
                .map(Alcance::resultado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean completa() {
        return faltantes.isEmpty() && fueraDeEscala.isEmpty();
    }
}
