package com.lautaro.multimetrico.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class MedicionTest {

    @Test
    void unaMedicionSinValorNoSePuedeCrear() {
        assertThatThrownBy(() -> new Medicion("u100", Metrica.FCR, YearMonth.of(2026, 8), null,
                LocalDate.parse("2026-08-22"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no se carga en cero");
    }

    @Test
    void unaMedicionSinFechaDeCorteNoSePuedeCrear() {
        assertThatThrownBy(() -> new Medicion("u100", Metrica.FCR, YearMonth.of(2026, 8),
                new BigDecimal("78.2"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha de corte");
    }

    @Test
    void laFechaDeCorteNoTieneQueCaerDentroDelPeriodo() {
        new Medicion("u100", Metrica.FCR, YearMonth.of(2026, 8), new BigDecimal("78.2"),
                LocalDate.parse("2026-09-03"), "importacion-17");
    }
}
