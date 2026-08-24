package com.lautaro.multimetrico.padron;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.padron.domain.Padron;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class PadronTest {

    private final Padron padron = new Padron(YearMonth.of(2026, 8), List.of(
            new Asesor("u100", "AGUIRRE FLAVIA YAMILA", "PERALTA SILVIA"),
            new Asesor("u200", "MIÑO HECTOR BAUTISTA", "PERALTA SILVIA"),
            new Asesor("u300", "SOSA ANA MARIA", "LEDESMA JORGE")));

    /**
     * El filtrado que hoy hace cada supervisor a mano, en cada archivo, todas las
     * semanas.
     */
    @Test
    void filtraElEquipoDeUnLider() {
        assertThat(padron.equipoDe("PERALTA SILVIA"))
                .extracting(Asesor::legajo)
                .containsExactly("u100", "u200");
    }

    @Test
    void elLiderSeBuscaNormalizadoIgualQueLosNombres() {
        assertThat(padron.equipoDe("peralta silvia")).hasSize(2);
    }

    @Test
    void listaLosLideresDeLaOperacion() {
        assertThat(padron.lideres()).containsExactly("LEDESMA JORGE", "PERALTA SILVIA");
    }

    @Test
    void unLegajoNoPuedeRepetirse() {
        assertThatThrownBy(() -> new Padron(YearMonth.of(2026, 8), List.of(
                new Asesor("u100", "AGUIRRE FLAVIA", "PERALTA SILVIA"),
                new Asesor("u100", "OTRA PERSONA", "PERALTA SILVIA"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("u100");
    }
}
