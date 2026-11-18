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

public final class EscenarioDeEjemplo {

    public static final String CAMPANIA = "SINTETICA";

    private EscenarioDeEjemplo() {
    }

    public static BigDecimal n(String valor) {
        return new BigDecimal(valor);
    }

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

    public static List<Medicion> mediciones(YearMonth periodo) {
        return List.of(
                medicion(periodo, "u100", Metrica.FCR, "70.5", 15),
                medicion(periodo, "u100", Metrica.FCR, "78.2", 22),
                medicion(periodo, "u100", Metrica.TMO, "612", 19),
                medicion(periodo, "u100", Metrica.NPS, "55.4", 22),
                medicion(periodo, "u200", Metrica.FCR, "61.0", 22),
                medicion(periodo, "u200", Metrica.TMO, "712", 19));
    }

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
