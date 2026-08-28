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

/**
 * El cálculo del tramo de arriba, banda por banda.
 *
 * <p>Vive en el mismo paquete que {@link MultimetricoServiceImpl} para poder
 * llamar al método directo: es una función pura y estática, y probarla a través
 * de un endpoint pediría una base entera para verificar una resta.
 */
class TramoDeArribaTest {

    private final Pliego pliego = pliego(YearMonth.of(2027, 1));

    /**
     * El alcance se arma acá y no se pide al pliego: lo único que necesita el
     * cálculo del tramo es en qué banda cayó el valor y cuánto paga.
     */
    private static Alcance alcanceDe(Escala escala, String valor) {
        Banda banda = escala.bandaPara(n(valor)).orElseThrow();
        return new Alcance(escala.metrica(), n(valor), banda.rango(), banda.resultado());
    }

    private Optional<TramoDeArriba> tramo(Metrica metrica, String valor) {
        Escala escala = pliego.escalaDe(metrica).orElseThrow();
        return MultimetricoServiceImpl.tramoDeArriba(escala, alcanceDe(escala, valor));
    }

    /**
     * FCR mejora subiendo. El objetivo es el borde de abajo de la banda de
     * arriba, y ese borde entra: llegar justo a 75 ya cuenta.
     */
    @Test
    void enUnaMetricaQueMejoraSubiendoElObjetivoEsElBordeQueSeAlcanza() {
        TramoDeArriba tramo = tramo(Metrica.FCR, "70.5").orElseThrow();

        assertThat(tramo.objetivo()).isEqualByComparingTo("75");
        assertThat(tramo.distancia()).isEqualByComparingTo("4.5");
        assertThat(tramo.condicion()).isEqualTo(CondicionDeTramo.ALCANZAR);
        assertThat(tramo.resultado()).isEqualByComparingTo("4");
    }

    /**
     * TMO mejora bajando. El objetivo es el borde de arriba de la banda de abajo,
     * y ese borde <b>no</b> entra: hay que quedar por debajo.
     */
    @Test
    void enUnaMetricaQueMejoraBajandoHayQueQuedarPorDebajoDelObjetivo() {
        TramoDeArriba tramo = tramo(Metrica.TMO, "612").orElseThrow();

        assertThat(tramo.objetivo()).isEqualByComparingTo("600");
        assertThat(tramo.distancia()).isEqualByComparingTo("12");
        assertThat(tramo.condicion()).isEqualTo(CondicionDeTramo.BAJAR_DE);
        assertThat(tramo.resultado()).isEqualByComparingTo("3");
    }

    /**
     * Clavado en el borde de abajo de su banda, no le falta nada medible: le
     * falta cruzar. Distancia 0 con condición de bajar es la lectura correcta,
     * y es lo que impide mostrar "te faltan 0" sin explicación.
     */
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

    /** Cuánto más suma si llega, que es el número que decide si vale la pena. */
    @Test
    void informaCuantoMasSumaSiLlegaAlTramoDeArriba() {
        assertThat(tramo(Metrica.FCR, "70.5").orElseThrow().mejora()).isEqualByComparingTo("2");
    }

    /**
     * El tramo de arriba es la banda que <b>paga más</b>, no la que sigue en la
     * lista. Acá la de al lado paga lo mismo que la actual: llegar hasta ahí no
     * cambiaría nada, así que el objetivo tiene que ser la de más allá.
     */
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
