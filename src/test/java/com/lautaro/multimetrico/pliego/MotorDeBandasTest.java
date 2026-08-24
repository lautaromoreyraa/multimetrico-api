package com.lautaro.multimetrico.pliego;

import static com.lautaro.multimetrico.pliego.PliegoDeEjemplo.n;
import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.pliego.domain.Banda;
import com.lautaro.multimetrico.pliego.domain.Escala;
import com.lautaro.multimetrico.pliego.domain.Evaluacion;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import com.lautaro.multimetrico.pliego.domain.Pliego;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * El borde de la banda es donde todo se rompe, así que es donde más se prueba.
 */
class MotorDeBandasTest {

    private final Escala escala = new Escala(Metrica.FCR, n("3.5"), List.of(
            new Banda(null, n("79.5"), n("-1.5")),
            new Banda(n("79.5"), n("80.1"), n("0")),
            new Banda(n("80.1"), null, n("3.5"))));

    @ParameterizedTest(name = "un valor de {0} paga {1}")
    @CsvSource({
            // El piso entra, el techo no: 79.5 es la primera de su banda y 80.1
            // ya pertenece a la de arriba.
            "79.4999, -1.5",
            "79.5,     0",
            "80.0999,  0",
            "80.1,     3.5",
            // El caso que en la planilla no caía en ninguna banda.
            "80.04,    0",
            // Extremos abiertos: nada se sale de la escala.
            "0,       -1.5",
            "100,      3.5",
            "180,      3.5"
    })
    void cadaValorCaeEnUnaSolaBanda(String valor, String esperado) {
        assertThat(escala.bandaPara(n(valor)))
                .isPresent()
                .get()
                .extracting(Banda::resultado)
                .isEqualTo(n(esperado));
    }

    /**
     * La escala compara con {@code compareTo} y no con {@code equals}: 80.10 y
     * 80.1 son el mismo número aunque sean distintos objetos. Con {@code equals}
     * de BigDecimal, un valor con un cero de más se caería de su banda.
     */
    @Test
    void losCerosDeMasNoCambianLaBanda() {
        assertThat(escala.bandaPara(n("80.100")))
                .isPresent()
                .get()
                .extracting(Banda::resultado)
                .isEqualTo(n("3.5"));
    }

    @Test
    void elAlcanceTotalSumaTodasLasMetricas() {
        Pliego pliego = PliegoDeEjemplo.sano();

        Evaluacion evaluacion = pliego.evaluar(Map.of(
                Metrica.NPS, n("80"),
                Metrica.FCR, n("60")));

        assertThat(evaluacion.alcanceTotal()).isEqualByComparingTo("4.5");
        assertThat(evaluacion.completa()).isTrue();
    }

    /**
     * Una métrica que todavía no llegó no vale cero: las métricas se actualizan
     * en fechas distintas y contarla como cero le inventaría un mal desempeño a
     * alguien por un archivo que no se subió.
     */
    @Test
    void loQueFaltaSeInformaComoFaltanteYNoComoCero() {
        Pliego pliego = PliegoDeEjemplo.sano();

        Evaluacion evaluacion = pliego.evaluar(Map.of(Metrica.NPS, n("80")));

        assertThat(evaluacion.alcanceTotal()).isEqualByComparingTo("3.5");
        assertThat(evaluacion.faltantes()).containsExactly(Metrica.FCR);
        assertThat(evaluacion.completa()).isFalse();
    }

    @Test
    void elAlcanceGuardaLaBandaEnQueCayo() {
        Pliego pliego = PliegoDeEjemplo.sano();

        Evaluacion evaluacion = pliego.evaluar(Map.of(Metrica.NPS, n("60")));

        assertThat(evaluacion.alcances().get(Metrica.NPS).banda().rango()).isEqualTo("[50, 75)");
    }

    @Test
    void elAlcanceMaximoEsLaSumaDeLosPesos() {
        assertThat(PliegoDeEjemplo.sano().alcanceMaximo()).isEqualByComparingTo("7.0");
    }

    /**
     * El mismo valor con dos pliegos distintos da resultados distintos. Es la
     * razón de guardar el pliego de cada mes: sin eso, recalcular un período
     * viejo con las bandas de hoy devuelve otro número.
     */
    @Test
    void elMismoValorPagaDistintoSiElPliegoCambio() {
        BigDecimal mismoNps = n("60");

        Pliego agosto = PliegoDeEjemplo.sano();
        Pliego septiembre = new Pliego(YearMonth.of(2026, 9), "CONVERGENTES", List.of(
                new Escala(Metrica.NPS, n("3.5"), List.of(
                        new Banda(null, n("65"), n("0")),
                        new Banda(n("65"), null, n("3.5"))))));

        assertThat(agosto.evaluar(Map.of(Metrica.NPS, mismoNps)).alcanceTotal()).isEqualByComparingTo("1");
        assertThat(septiembre.evaluar(Map.of(Metrica.NPS, mismoNps)).alcanceTotal()).isEqualByComparingTo("0");
    }
}
