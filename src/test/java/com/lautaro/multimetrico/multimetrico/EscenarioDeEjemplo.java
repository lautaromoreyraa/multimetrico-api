package com.lautaro.multimetrico.multimetrico;

import com.lautaro.multimetrico.medicion.domain.Medicion;
import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.padron.domain.Padron;
import com.lautaro.multimetrico.pliego.domain.Banda;
import com.lautaro.multimetrico.pliego.domain.Escala;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import com.lautaro.multimetrico.pliego.domain.Pliego;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Un mes de mentira, entero: pliego, padrón y mediciones.
 *
 * <p><b>Todo es inventado.</b> Los umbrales tienen la forma de los reales —tres
 * tramos por métrica, pesos que suman un techo— pero no son los de ninguna
 * operación, y las personas no existen. El motor no sabe de dónde salen los
 * números.
 *
 * <p>El pliego mezcla los dos sentidos a propósito, porque es donde se rompe el
 * cálculo del tramo de arriba: en FCR y NPS se mejora subiendo y en TMO,
 * bajando.
 */
final class EscenarioDeEjemplo {

    static final String CAMPANIA = "SINTETICA";

    private EscenarioDeEjemplo() {
    }

    static BigDecimal n(String valor) {
        return new BigDecimal(valor);
    }

    /**
     * Pliego de tres métricas. Alcance máximo: 10,5.
     *
     * <ul>
     *   <li>FCR sube: menos de 60 no paga, de 60 a 75 paga 2, de 75 para arriba paga 4.</li>
     *   <li>TMO baja: menos de 600 paga 3, de 600 a 700 paga 1, de 700 para arriba no paga.</li>
     *   <li>NPS sube: menos de 50 no paga, de 50 para arriba paga 3,5.</li>
     * </ul>
     */
    static Pliego pliego(YearMonth periodo) {
        return new Pliego(periodo, CAMPANIA, List.of(
                new Escala(Metrica.FCR, n("4"), List.of(
                        new Banda(null, n("60"), n("0")),
                        new Banda(n("60"), n("75"), n("2")),
                        new Banda(n("75"), null, n("4")))),
                new Escala(Metrica.TMO, n("3"), List.of(
                        new Banda(null, n("600"), n("3")),
                        new Banda(n("600"), n("700"), n("1")),
                        new Banda(n("700"), null, n("0")))),
                new Escala(Metrica.NPS, n("3.5"), List.of(
                        new Banda(null, n("50"), n("0")),
                        new Banda(n("50"), null, n("3.5"))))));
    }

    static Padron padron(YearMonth periodo) {
        return new Padron(periodo, List.of(
                new Asesor("u100", "AGUIRRE FLAVIA YAMILA", "PERALTA SILVIA"),
                new Asesor("u200", "MIÑO HECTOR BAUTISTA", "PERALTA SILVIA"),
                new Asesor("u300", "SOSA ANA MARIA", "LEDESMA JORGE")));
    }

    static Medicion medicion(YearMonth periodo, String legajo, Metrica metrica, String valor, int diaDeCorte) {
        return new Medicion(legajo, metrica, periodo, n(valor), periodo.atDay(diaDeCorte), null);
    }

    static LocalDate corte(YearMonth periodo, int dia) {
        return periodo.atDay(dia);
    }

    /**
     * Las mediciones del equipo de PERALTA SILVIA, con cortes distintos por
     * métrica: es la situación normal —cada archivo se publica por su lado— y es
     * la que la planilla junta sin decirlo.
     *
     * <p>u200 no tiene NPS: queda faltante, que no es lo mismo que valer cero.
     */
    static List<Medicion> mediciones(YearMonth periodo) {
        return List.of(
                medicion(periodo, "u100", Metrica.FCR, "70.5", 15),
                medicion(periodo, "u100", Metrica.FCR, "78.2", 22),
                medicion(periodo, "u100", Metrica.TMO, "612", 19),
                medicion(periodo, "u100", Metrica.NPS, "55.4", 22),
                medicion(periodo, "u200", Metrica.FCR, "61.0", 22),
                medicion(periodo, "u200", Metrica.TMO, "712", 19));
    }
}
