package com.lautaro.multimetrico.fixture;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Banda;
import com.lautaro.multimetrico.domain.Escala;
import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.medicion.MedicionVigente;
import com.lautaro.multimetrico.dto.medicion.MedicionesVigentes;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
public final class EscenarioDeEjemplo {

    public static final String CAMPANIA = "SINTETICA";

    private EscenarioDeEjemplo() {
    }

    public static BigDecimal n(String valor) {
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
    public static Pliego pliego(YearMonth periodo) {
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

    public static Padron padron(YearMonth periodo) {
        return new Padron(periodo, List.of(
                new Asesor("u100", "AGUIRRE FLAVIA YAMILA", "PERALTA SILVIA"),
                new Asesor("u200", "MIÑO HECTOR BAUTISTA", "PERALTA SILVIA"),
                new Asesor("u300", "SOSA ANA MARIA", "LEDESMA JORGE")));
    }

    public static Medicion medicion(YearMonth periodo, String legajo, Metrica metrica,
            String valor, int diaDeCorte) {
        return new Medicion(legajo, metrica, periodo, n(valor), periodo.atDay(diaDeCorte), null);
    }

    public static LocalDate corte(YearMonth periodo, int dia) {
        return periodo.atDay(dia);
    }

    /**
     * Las mediciones del equipo de PERALTA SILVIA, con cortes distintos por
     * métrica: es la situación normal —cada archivo se publica por su lado— y es
     * la que la planilla junta sin decirlo.
     *
     * <p>u200 no tiene NPS: queda faltante, que no es lo mismo que valer cero.
     */
    public static List<Medicion> mediciones(YearMonth periodo) {
        return List.of(
                medicion(periodo, "u100", Metrica.FCR, "70.5", 15),
                medicion(periodo, "u100", Metrica.FCR, "78.2", 22),
                medicion(periodo, "u100", Metrica.TMO, "612", 19),
                medicion(periodo, "u100", Metrica.NPS, "55.4", 22),
                medicion(periodo, "u200", Metrica.FCR, "61.0", 22),
                medicion(periodo, "u200", Metrica.TMO, "712", 19));
    }

    /**
     * Vigentes armadas a mano, sin pasar por el desempate.
     *
     * <p>Los tests que arman una fila del multimétrico no están probando cuál de
     * dos cargas gana —eso es de {@code MedicionService} y tiene sus propios
     * tests—, así que le dan el resultado ya resuelto y se ahorran una base.
     */
    public static MedicionesVigentes vigentes(YearMonth periodo, Object... metricaValorDia) {
        Map<Metrica, MedicionVigente> porMetrica = new EnumMap<>(Metrica.class);
        for (int i = 0; i < metricaValorDia.length; i += 3) {
            porMetrica.put(
                    (Metrica) metricaValorDia[i],
                    new MedicionVigente(n((String) metricaValorDia[i + 1]),
                            periodo.atDay((Integer) metricaValorDia[i + 2])));
        }
        return porMetrica.isEmpty() ? MedicionesVigentes.ninguna() : new MedicionesVigentes(porMetrica);
    }
}
