package com.lautaro.multimetrico.pliego;

import com.lautaro.multimetrico.pliego.domain.Banda;
import com.lautaro.multimetrico.pliego.domain.Escala;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import com.lautaro.multimetrico.pliego.domain.Pliego;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Pliegos armados a mano para los tests.
 *
 * <p><b>Los números son inventados.</b> Tienen la misma forma que los de la
 * operación que inspiró el proyecto —cinco métricas, bandas escalonadas, pesos
 * que suman un techo— pero no son sus umbrales: esos son objetivos internos de
 * una empresa y no van en un repositorio público. Lo que se prueba acá es el
 * motor, y el motor no sabe de dónde salen los números.
 */
final class PliegoDeEjemplo {

    private PliegoDeEjemplo() {
    }

    static BigDecimal n(String valor) {
        return new BigDecimal(valor);
    }

    /** Escala sana de tres bandas, abierta en los dos extremos. */
    static Escala escalaSana(Metrica metrica, String peso) {
        return new Escala(metrica, n(peso), List.of(
                new Banda(null, n("50"), n("0")),
                new Banda(n("50"), n("75"), n("1")),
                new Banda(n("75"), null, n(peso))));
    }

    static Pliego sano() {
        return new Pliego(YearMonth.of(2026, 8), "CONVERGENTES", List.of(
                escalaSana(Metrica.NPS, "3.5"),
                escalaSana(Metrica.FCR, "3.5")));
    }

    static Pliego conUnaSolaEscala(Escala escala) {
        return new Pliego(YearMonth.of(2026, 8), "CONVERGENTES", List.of(escala));
    }
}
