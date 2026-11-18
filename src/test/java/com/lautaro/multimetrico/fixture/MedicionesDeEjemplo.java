package com.lautaro.multimetrico.fixture;

import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public final class MedicionesDeEjemplo {

    private MedicionesDeEjemplo() {
    }

    public static Medicion medicion(YearMonth periodo, String legajo, Metrica metrica,
            String valor, String corte) {
        return new Medicion(legajo, metrica, periodo, new BigDecimal(valor), LocalDate.parse(corte), null);
    }
}
