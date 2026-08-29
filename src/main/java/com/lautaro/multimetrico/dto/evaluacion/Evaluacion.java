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
 * <p>Los faltantes y los que quedaron fuera de escala <b>viajan aparte</b>, y esa
 * separación es lo único que este record decide. No es lo mismo "no hay archivo"
 * que "hay un valor que paga cero": el primero no tiene valor que mostrar y el
 * segundo sí, y una vez aplastados no se pueden volver a separar.
 *
 * <p>Lo que <b>cobra</b> una métrica sin archivo es otra pregunta, y la contesta
 * la regla de negocio, no este record: hoy es <b>cero</b>. Si el archivo de NPS
 * no llegó al día 10, el NPS no bonifica. Por eso {@code alcanceTotal} suma solo
 * lo presente —que da exactamente lo mismo que sumar cero por lo ausente— y la
 * fila no queda fuera de ningún promedio.
 *
 * <p>{@code alcanceTotal} y {@code completa} vienen calculados y no se derivan
 * acá. Quien evalúa es {@code PliegoServiceImpl}: este record es el resultado,
 * no el que lo produce.
 *
 * @param alcanceTotal lo que se cobra: la suma de lo calculado, que es la suma
 *     total porque lo que falta aporta cero
 * @param completa si no falta ninguna métrica y ninguna quedó fuera de escala.
 *     Describe la <b>cobertura del dato</b>, no la validez del total: un total
 *     de una fila incompleta es igual de válido, simplemente hubo métricas que
 *     no bonificaron.
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
