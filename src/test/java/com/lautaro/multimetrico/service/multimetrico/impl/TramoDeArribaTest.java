package com.lautaro.multimetrico.service.multimetrico.impl;

import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.n;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.pliego;
import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.domain.Banda;
import com.lautaro.multimetrico.domain.Escala;
import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.evaluacion.Alcance;
import com.lautaro.multimetrico.dto.multimetrico.TramoDeArriba;
import com.lautaro.multimetrico.enumeration.CondicionDeTramo;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TramoDeArribaTest {

    private final Pliego pliego = pliego(YearMonth.of(2027, 1));

    private static Alcance alcanceDe(Escala escala, String valor) {
        Banda banda = escala.bandaPara(n(valor)).orElseThrow();
        return new Alcance(escala.metrica(), n(valor), banda.rango(), banda.resultado());
    }

    private Optional<TramoDeArriba> tramo(Metrica metrica, String valor) {
        Escala escala = pliego.escalaDe(metrica).orElseThrow();
        return MultimetricoServiceImpl.tramoDeArriba(escala, alcanceDe(escala, valor));
    }

    @Test
    void enUnaMetricaQueMejoraSubiendoElObjetivoEsElBordeQueSeAlcanza() {
        TramoDeArriba tramo = tramo(Metrica.FCR, "70.5").orElseThrow();

        assertThat(tramo.objetivo()).isEqualByComparingTo("75");
        assertThat(tramo.distancia()).isEqualByComparingTo("4.5");
        assertThat(tramo.condicion()).isEqualTo(CondicionDeTramo.ALCANZAR);
        assertThat(tramo.resultado()).isEqualByComparingTo("4");
    }

    @Test
    void enUnaMetricaQueMejoraBajandoHayQueQuedarPorDebajoDelObjetivo() {
        TramoDeArriba tramo = tramo(Metrica.TMO, "612").orElseThrow();

        assertThat(tramo.objetivo()).isEqualByComparingTo("600");
        assertThat(tramo.distancia()).isEqualByComparingTo("12");
        assertThat(tramo.condicion()).isEqualTo(CondicionDeTramo.BAJAR_DE);
        assertThat(tramo.resultado()).isEqualByComparingTo("3");
    }

    @Test
    void elQueEstaJustoSobreElBordeTieneDistanciaCeroPeroTodaviaNoEntro() {
        TramoDeArriba tramo = tramo(Metrica.TMO, "600").orElseThrow();

        assertThat(tramo.distancia()).isEqualByComparingTo("0");
        assertThat(tramo.condicion()).isEqualTo(CondicionDeTramo.BAJAR_DE);
    }

    @Test
    void elQueYaEstaEnLaMejorBandaNoTieneTramoDeArriba() {
        assertThat(tramo(Metrica.FCR, "82")).isEmpty();
        assertThat(tramo(Metrica.TMO, "540")).isEmpty();
    }

    @Test
    void informaCuantoMasSumaSiLlegaAlTramoDeArriba() {
        assertThat(tramo(Metrica.FCR, "70.5").orElseThrow().mejora()).isEqualByComparingTo("2");
    }

    @Test
    void saltaLaBandaDeAlLadoSiPagaLoMismoQueLaActual() {
        Escala escalonada = new Escala(Metrica.FCR, n("4"), List.of(
                new Banda(null, n("60"), n("2")),
                new Banda(n("60"), n("75"), n("2")),
                new Banda(n("75"), null, n("4"))));

        TramoDeArriba tramo = MultimetricoServiceImpl
                .tramoDeArriba(escalonada, alcanceDe(escalonada, "55"))
                .orElseThrow();

        assertThat(tramo.objetivo()).isEqualByComparingTo("75");
        assertThat(tramo.distancia()).isEqualByComparingTo(new BigDecimal("20"));
    }
}
