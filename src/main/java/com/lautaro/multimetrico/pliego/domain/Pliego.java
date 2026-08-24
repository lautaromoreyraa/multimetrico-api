package com.lautaro.multimetrico.pliego.domain;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Los objetivos de un mes: qué métricas se miden, con qué bandas y con qué peso.
 *
 * <p>Es la entidad central del sistema, y la razón está en el proceso que
 * reemplaza. En la planilla hay <em>una</em> hoja de pliego: la del mes vigente.
 * Cada mes se pisa con la nueva, así que los pliegos anteriores dejan de existir
 * y el cálculo de un mes viejo no se puede reproducir — aplicarle las bandas de
 * hoy a los datos de marzo da otro número.
 *
 * <p>Acá el pliego está atado a su período y no se pisa nunca. Recalcular marzo
 * usa el pliego de marzo. De ahí sale, además, la única lectura que la planilla
 * no puede dar: si un asesor mantuvo su NPS y cobró menos, se puede separar
 * cuánto se movió él de cuánto se movió la vara.
 *
 * <p>La campaña está desde el principio aunque hoy se use una sola. Los
 * multimétricos de la operación ya vienen separados por segmento, así que es
 * cuestión de tiempo que dos campañas convivan con objetivos distintos en el
 * mismo mes; cuesta un campo ahora y un rediseño después.
 */
public record Pliego(YearMonth periodo, String campania, List<Escala> escalas) {

    public Pliego {
        if (periodo == null) {
            throw new IllegalArgumentException("El pliego necesita un período");
        }
        if (campania == null || campania.isBlank()) {
            throw new IllegalArgumentException("El pliego necesita una campaña");
        }
        if (escalas == null || escalas.isEmpty()) {
            throw new IllegalArgumentException("El pliego no tiene escalas");
        }
        escalas = List.copyOf(escalas);
    }

    public Optional<Escala> escalaDe(Metrica metrica) {
        return escalas.stream().filter(escala -> escala.metrica() == metrica).findFirst();
    }

    /** El alcance máximo posible: la suma de los pesos de todas las escalas. */
    public BigDecimal alcanceMaximo() {
        return escalas.stream()
                .map(Escala::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Evalúa las mediciones de un asesor contra este pliego.
     *
     * <p>Acepta que falten métricas. Es lo normal y no un error: las métricas se
     * actualizan por separado y cada una tiene su propia fecha de corte, así que
     * a mitad de semana es habitual tener el TMO al viernes pasado y el FCR al
     * lunes anterior. Lo que falta se informa como faltante en vez de contarse
     * como cero, que sería inventarle un mal desempeño a alguien por un archivo
     * que todavía no llegó.
     */
    public Evaluacion evaluar(Map<Metrica, BigDecimal> mediciones) {
        Map<Metrica, Alcance> alcances = new EnumMap<>(Metrica.class);
        List<Metrica> faltantes = new java.util.ArrayList<>();
        List<Metrica> fueraDeEscala = new java.util.ArrayList<>();

        for (Escala escala : escalas) {
            BigDecimal valor = mediciones.get(escala.metrica());
            if (valor == null) {
                faltantes.add(escala.metrica());
                continue;
            }
            escala.bandaPara(valor).ifPresentOrElse(
                    banda -> alcances.put(escala.metrica(), Alcance.de(escala.metrica(), valor, banda)),
                    () -> fueraDeEscala.add(escala.metrica()));
        }

        return new Evaluacion(periodo, campania, Map.copyOf(alcances), List.copyOf(faltantes),
                List.copyOf(fueraDeEscala));
    }
}
