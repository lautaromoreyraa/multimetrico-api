package com.lautaro.multimetrico.medicion.domain;

import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lo que vale hoy cada métrica de un asesor, sacado de toda su historia del mes.
 *
 * <p>Es el paso que separa "guardo todas las cargas" de "muestro un número". La
 * historia tiene varias mediciones por métrica —una por corte— y el multimétrico
 * necesita exactamente una: la del corte más reciente.
 *
 * <p>Lo importante es que <b>la fecha viaja pegada al valor</b>. Podría devolver
 * solo un {@code Map<Metrica, BigDecimal>} y alcanzaría para evaluar contra el
 * pliego, pero entonces se perdería el "actualizado al" de cada métrica, que es
 * justo el dato que la planilla pierde: junta un TMO al viernes con un FCR al
 * lunes anterior y los muestra como si fueran la misma foto. Acá el valor no se
 * puede sacar sin su corte.
 *
 * <p><b>Una métrica ausente está ausente.</b> No se completa con cero. Un cero
 * es un desempeño malísimo y un faltante es un archivo que todavía no llegó;
 * confundirlos le inventa a alguien un mes que no tuvo.
 */
public record MedicionesVigentes(Map<Metrica, Medicion> porMetrica) {

    private static final MedicionesVigentes NINGUNA = new MedicionesVigentes(Collections.emptyMap());

    public MedicionesVigentes {
        porMetrica = porMetrica == null || porMetrica.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new EnumMap<>(porMetrica));
    }

    public static MedicionesVigentes ninguna() {
        return NINGUNA;
    }

    /**
     * Se queda con la última de cada métrica.
     *
     * <p>{@code historia} tiene que venir <b>en orden de carga</b>: el desempate
     * entre dos mediciones del mismo corte lo gana la que llegó después, que es
     * la corrección. Ver {@link Medicion#desplazaA(Medicion)}.
     */
    public static MedicionesVigentes de(List<Medicion> historia) {
        Map<Metrica, Medicion> vigentes = new EnumMap<>(Metrica.class);
        for (Medicion medicion : historia) {
            Medicion actual = vigentes.get(medicion.metrica());
            if (actual == null || medicion.desplazaA(actual)) {
                vigentes.put(medicion.metrica(), medicion);
            }
        }
        return new MedicionesVigentes(vigentes);
    }

    public Optional<Medicion> de(Metrica metrica) {
        return Optional.ofNullable(porMetrica.get(metrica));
    }

    /** A qué día está la métrica. Vacío si no hay medición: no hay fecha que inventar. */
    public Optional<LocalDate> corteDe(Metrica metrica) {
        return de(metrica).map(Medicion::fechaDeCorte);
    }

    /**
     * Los valores sueltos, en la forma que espera
     * {@code Pliego.evaluar(Map)}.
     *
     * <p>Las métricas que no están, no aparecen en el mapa — y no aparecen en
     * cero. El pliego las va a informar como faltantes.
     */
    public Map<Metrica, BigDecimal> valores() {
        Map<Metrica, BigDecimal> valores = new EnumMap<>(Metrica.class);
        porMetrica.forEach((metrica, medicion) -> valores.put(metrica, medicion.valor()));
        return valores;
    }

    public boolean vacia() {
        return porMetrica.isEmpty();
    }
}
