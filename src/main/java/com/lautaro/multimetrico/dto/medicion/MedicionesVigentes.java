package com.lautaro.multimetrico.dto.medicion;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lo que vale hoy cada métrica de un asesor.
 *
 * <p>Es el paso que separa "guardo todas las cargas" de "muestro un número". La
 * historia tiene varias mediciones por métrica —una por corte— y el multimétrico
 * necesita exactamente una: la del corte más reciente.
 *
 * <p><b>Quedarse con esa una es trabajo de {@code MedicionService}</b>, no de
 * este record. Acá llega ya resuelto; los métodos de abajo son proyecciones sobre
 * el mapa, no el criterio de desempate.
 *
 * <p><b>Una métrica ausente está ausente.</b> No se completa con cero. Un cero
 * es un desempeño malísimo y un faltante es un archivo que todavía no llegó;
 * confundirlos le inventa a alguien un mes que no tuvo.
 */
public record MedicionesVigentes(Map<Metrica, MedicionVigente> porMetrica) {

    private static final MedicionesVigentes NINGUNA = new MedicionesVigentes(Collections.emptyMap());

    public MedicionesVigentes {
        porMetrica = porMetrica == null || porMetrica.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new EnumMap<>(porMetrica));
    }

    /** El asesor que no tiene ninguna medición cargada; aparece igual, con todo faltante. */
    public static MedicionesVigentes ninguna() {
        return NINGUNA;
    }

    public Optional<MedicionVigente> de(Metrica metrica) {
        return Optional.ofNullable(porMetrica.get(metrica));
    }

    /** A qué día está la métrica. Vacío si no hay medición: no hay fecha que inventar. */
    public Optional<LocalDate> corteDe(Metrica metrica) {
        return de(metrica).map(MedicionVigente::fechaDeCorte);
    }

    /**
     * Los valores sueltos, en la forma que espera la evaluación contra el pliego.
     *
     * <p>Las métricas que no están, no aparecen en el mapa — y no aparecen en
     * cero. El pliego las va a informar como faltantes.
     */
    public Map<Metrica, BigDecimal> valores() {
        Map<Metrica, BigDecimal> valores = new EnumMap<>(Metrica.class);
        porMetrica.forEach((metrica, vigente) -> valores.put(metrica, vigente.valor()));
        return valores;
    }

    public boolean vacia() {
        return porMetrica.isEmpty();
    }
}
