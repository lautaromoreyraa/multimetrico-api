package com.lautaro.multimetrico.multimetrico.domain;

import com.lautaro.multimetrico.medicion.domain.MedicionesVigentes;
import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.pliego.domain.Alcance;
import com.lautaro.multimetrico.pliego.domain.Evaluacion;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import com.lautaro.multimetrico.pliego.domain.Pliego;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * La fila de una persona en el multimétrico: todas sus métricas del mes, en qué
 * tramo cayó cada una, cuánto suma en total y qué le falta.
 *
 * <p>Es el producto. Todo lo demás del sistema existe para poder armarla sin las
 * tres pérdidas de la planilla: el pliego que se pisó y ya no se puede
 * reproducir, las filas que no empalmaron por nombre y desaparecieron sin aviso,
 * y las fechas de corte que se juntaron sin decirlo.
 *
 * <p>Se arma con {@link #armar(Pliego, Asesor, MedicionesVigentes)}, que es una
 * función pura: mismo pliego, mismo asesor y mismas mediciones dan siempre la
 * misma fila, sin importar cuándo se la pida. Esa es la propiedad que la planilla
 * no tiene y por la que recalcular marzo dentro de seis meses devuelve lo mismo
 * que devolvió en marzo.
 *
 * <h2>Los faltantes viajan aparte y no suman cero</h2>
 *
 * <p>{@code alcanceTotal} es la suma de lo que se pudo calcular. Un total de 9%
 * con dos métricas sin cargar no es lo mismo que un 9% con todo cargado, y el
 * que lo mira tiene que poder distinguirlo: por eso {@code faltantes} está al
 * lado del total y no escondido. Rellenar con ceros daría un número que se ve
 * completo y es mentira.
 */
public record FilaDeMultimetrico(
        Asesor asesor,
        List<CeldaDeMultimetrico> celdas,
        List<Metrica> faltantes,
        List<Metrica> fueraDeEscala,
        BigDecimal alcanceTotal) {

    public FilaDeMultimetrico {
        celdas = List.copyOf(celdas);
        faltantes = List.copyOf(faltantes);
        fueraDeEscala = List.copyOf(fueraDeEscala);
    }

    /**
     * Evalúa las mediciones vigentes de un asesor contra el pliego del período.
     *
     * <p>El pliego que entra tiene que ser el del período que se está armando,
     * no el vigente hoy. Quien lo elige es el servicio; esta función confía en
     * que ya se eligió bien, y por eso mismo no toca ningún repositorio.
     */
    public static FilaDeMultimetrico armar(Pliego pliego, Asesor asesor, MedicionesVigentes vigentes) {
        Evaluacion evaluacion = pliego.evaluar(vigentes.valores());

        List<CeldaDeMultimetrico> celdas = new ArrayList<>();
        for (Alcance alcance : evaluacion.alcances().values()) {
            TramoDeArriba tramo = pliego.escalaDe(alcance.metrica())
                    .flatMap(escala -> TramoDeArriba.de(escala, alcance))
                    .orElse(null);

            // El corte está sí o sí: el alcance existe porque hubo una medición
            // vigente, y toda medición tiene fecha de corte.
            LocalDate corte = vigentes.corteDe(alcance.metrica()).orElseThrow();
            celdas.add(new CeldaDeMultimetrico(alcance, corte, tramo));
        }
        celdas.sort(Comparator.comparing(celda -> celda.metrica().ordinal()));

        return new FilaDeMultimetrico(asesor, celdas, evaluacion.faltantes(), evaluacion.fueraDeEscala(),
                evaluacion.alcanceTotal());
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

    /** El corte más viejo de la fila: hasta dónde llega, de verdad, este número. */
    public Optional<LocalDate> corteMasViejo() {
        return celdas.stream().map(CeldaDeMultimetrico::actualizadoAl).min(Comparator.naturalOrder());
    }

    public Optional<LocalDate> corteMasNuevo() {
        return celdas.stream().map(CeldaDeMultimetrico::actualizadoAl).max(Comparator.naturalOrder());
    }

    /** Todas las métricas del pliego tienen medición y todas cayeron en una banda. */
    public boolean completa() {
        return faltantes.isEmpty() && fueraDeEscala.isEmpty();
    }
}
