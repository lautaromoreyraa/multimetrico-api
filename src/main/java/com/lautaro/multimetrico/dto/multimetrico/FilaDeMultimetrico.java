package com.lautaro.multimetrico.dto.multimetrico;

import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * La fila de una persona en el multimétrico: todas sus métricas del mes, en qué
 * tramo cayó cada una, cuánto suma en total y qué le falta.
 *
 * <p>Es el producto. Todo lo demás del sistema existe para poder armarla sin las
 * tres pérdidas de la planilla: el pliego que se pisó y ya no se puede
 * reproducir, las filas que no empalmaron por nombre y desaparecieron sin aviso,
 * y las fechas de corte que se juntaron sin decirlo.
 *
 * <p>La arma {@code MultimetricoServiceImpl} y el cálculo es una función pura:
 * mismo pliego, mismo asesor y mismas mediciones dan siempre la misma fila, sin
 * importar cuándo se la pida. Esa es la propiedad que la planilla no tiene y por
 * la que recalcular marzo dentro de seis meses devuelve lo mismo que devolvió en
 * marzo.
 *
 * <h2>Los faltantes viajan aparte y no suman cero</h2>
 *
 * <p>{@code alcanceTotal} es la suma de lo que se pudo calcular. Un total de 9%
 * con dos métricas sin cargar no es lo mismo que un 9% con todo cargado, y el
 * que lo mira tiene que poder distinguirlo: por eso {@code faltantes} está al
 * lado del total y no escondido. Rellenar con ceros daría un número que se ve
 * completo y es mentira.
 *
 * @param completa todas las métricas del pliego tienen medición y todas cayeron
 *     en una banda
 * @param corteMasViejo hasta dónde llega, de verdad, este número
 */
public record FilaDeMultimetrico(
        AsesorResponse asesor,
        List<CeldaDeMultimetrico> celdas,
        List<Metrica> faltantes,
        List<Metrica> fueraDeEscala,
        BigDecimal alcanceTotal,
        boolean completa,
        LocalDate corteMasViejo,
        LocalDate corteMasNuevo) {

    public FilaDeMultimetrico {
        celdas = List.copyOf(celdas);
        faltantes = List.copyOf(faltantes);
        fueraDeEscala = List.copyOf(fueraDeEscala);
    }

    /**
     * La fecha de corte de cada métrica.
     *
     * <p>Existe como mapa además de estar en cada celda porque es la lectura que
     * más importa cuando se mira una fila entera: si acá hay tres fechas
     * distintas, el "total" del mes es un promedio de tres momentos y conviene
     * decirlo.
     */
    public Map<Metrica, LocalDate> cortesPorMetrica() {
        Map<Metrica, LocalDate> cortes = new EnumMap<>(Metrica.class);
        celdas.forEach(celda -> cortes.put(celda.metrica(), celda.actualizadoAl()));
        return cortes;
    }
}
