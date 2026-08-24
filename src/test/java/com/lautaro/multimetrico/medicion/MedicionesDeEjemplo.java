package com.lautaro.multimetrico.medicion;

import com.lautaro.multimetrico.medicion.domain.Medicion;
import com.lautaro.multimetrico.pliego.domain.Metrica;
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
final class MedicionesDeEjemplo {

    static final YearMonth AGOSTO = YearMonth.of(2026, 8);

    private MedicionesDeEjemplo() {
    }

    static Medicion medicion(String legajo, Metrica metrica, String valor, String corte) {
        return new Medicion(legajo, metrica, AGOSTO, new BigDecimal(valor), LocalDate.parse(corte), null);
    }
}
