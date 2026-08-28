package com.lautaro.multimetrico.fixture;

import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Mediciones armadas a mano para los tests.
 *
 * <p><b>Todo es inventado</b>: los legajos, los valores y las fechas. Tienen la
 * forma de los reales —un corte por semana, valores que se mueven de a poco—
 * pero no son los de ninguna operación.
 */
public final class MedicionesDeEjemplo {

    private MedicionesDeEjemplo() {
    }

    public static Medicion medicion(YearMonth periodo, String legajo, Metrica metrica,
            String valor, String corte) {
        return new Medicion(legajo, metrica, periodo, new BigDecimal(valor), LocalDate.parse(corte), null);
    }
}
