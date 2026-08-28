package com.lautaro.multimetrico.dto.evaluacion;

import com.lautaro.multimetrico.enumeration.Metrica;
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
 *
 * <p>{@code alcanceTotal} y {@code completa} vienen calculados y no se derivan
 * acá. Quien evalúa es {@code PliegoServiceImpl}: este record es el resultado,
 * no el que lo produce.
 *
 * @param alcanceTotal la suma de lo que efectivamente se pudo calcular
 * @param completa si no falta ninguna métrica y ninguna quedó fuera de escala
 */
public record Evaluacion(
        YearMonth periodo,
        String campania,
        Map<Metrica, Alcance> alcances,
        List<Metrica> faltantes,
        List<Metrica> fueraDeEscala,
        BigDecimal alcanceTotal,
        boolean completa) {
}
