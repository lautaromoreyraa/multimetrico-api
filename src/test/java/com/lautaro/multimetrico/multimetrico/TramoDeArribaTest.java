package com.lautaro.multimetrico.multimetrico;

import static com.lautaro.multimetrico.multimetrico.EscenarioDeEjemplo.n;
import static com.lautaro.multimetrico.multimetrico.EscenarioDeEjemplo.pliego;
import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.multimetrico.domain.TramoDeArriba;
import com.lautaro.multimetrico.pliego.domain.Alcance;
import com.lautaro.multimetrico.pliego.domain.Banda;
import com.lautaro.multimetrico.pliego.domain.Escala;
import com.lautaro.multimetrico.pliego.domain.Evaluacion;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import com.lautaro.multimetrico.pliego.domain.Pliego;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TramoDeArribaTest {

    private final Pliego pliego = pliego(YearMonth.of(2027, 1));

    private Optional<TramoDeArriba> tramo(Metrica metrica, String valor) {
        Evaluacion evaluacion = pliego.evaluar(Map.of(metrica, n(valor)));
        Alcance alcance = evaluacion.alcances().get(metrica);
        return TramoDeArriba.de(pliego.escalaDe(metrica).orElseThrow(), alcance);
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
        assertThat(tramo.condicion()).isEqualTo(TramoDeArriba.Condicion.ALCANZAR);
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
        assertThat(tramo.condicion()).isEqualTo(TramoDeArriba.Condicion.BAJAR_DE);
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
        assertThat(tramo.condicion()).isEqualTo(TramoDeArriba.Condicion.BAJAR_DE);
    }

    @Test
    void elQueYaEstaEnLaMejorBandaNoTieneTramoDeArriba() {
        assertThat(tramo(Metrica.FCR, "82")).isEmpty();
        assertThat(tramo(Metrica.TMO, "540")).isEmpty();
    }

    /** Cuánto más suma si llega, que es el número que decide si vale la pena. */
    @Test
    void informaCuantoMasSumaSiLlegaAlTramoDeArriba() {
        Alcance alcance = pliego.evaluar(Map.of(Metrica.FCR, n("70.5"))).alcances().get(Metrica.FCR);
        TramoDeArriba tramo = TramoDeArriba.de(pliego.escalaDe(Metrica.FCR).orElseThrow(), alcance).orElseThrow();

        assertThat(tramo.mejora(alcance)).isEqualByComparingTo("2");
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

        Pliego conMeseta = new Pliego(YearMonth.of(2027, 1), "SINTETICA", List.of(escalonada));
        Alcance alcance = conMeseta.evaluar(Map.of(Metrica.FCR, n("55"))).alcances().get(Metrica.FCR);

        TramoDeArriba tramo = TramoDeArriba.de(escalonada, alcance).orElseThrow();

        assertThat(tramo.objetivo()).isEqualByComparingTo("75");
        assertThat(tramo.distancia()).isEqualByComparingTo(new BigDecimal("20"));
    }
}
