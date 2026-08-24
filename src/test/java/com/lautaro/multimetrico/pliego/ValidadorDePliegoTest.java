package com.lautaro.multimetrico.pliego;

import static com.lautaro.multimetrico.pliego.PliegoDeEjemplo.n;
import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.pliego.domain.Banda;
import com.lautaro.multimetrico.pliego.domain.Escala;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import com.lautaro.multimetrico.pliego.validacion.ValidadorDePliego;
import com.lautaro.multimetrico.pliego.validacion.Violacion;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Los dos defectos que originaron el proyecto —bandas superpuestas y huecos
 * entre bandas— tienen que ser imposibles de guardar.
 */
class ValidadorDePliegoTest {

    private final ValidadorDePliego validador = new ValidadorDePliego();

    @Test
    void unPliegoSanoNoTieneViolaciones() {
        assertThat(validador.validar(PliegoDeEjemplo.sano())).isEmpty();
    }

    /**
     * El caso real: una banda termina en 681 y la siguiente arranca en 680, así
     * que 680 pertenece a las dos. En la planilla lo resuelve la primera
     * coincidencia, en silencio.
     */
    @Test
    void detectaBandasSuperpuestas() {
        Escala superpuesta = new Escala(Metrica.TMO, n("3"), List.of(
                new Banda(null, n("681"), n("3")),
                new Banda(n("680"), null, n("0"))));

        List<Violacion> violaciones = validador.validar(PliegoDeEjemplo.conUnaSolaEscala(superpuesta));

        assertThat(violaciones).hasSize(1);
        assertThat(violaciones.getFirst().tipo()).isEqualTo(Violacion.Tipo.SUPERPOSICION);
        assertThat(violaciones.getFirst().mensaje()).contains("680", "681");
    }

    /**
     * El otro caso real: las bandas se escriben con el número redondeado para
     * mostrar, y entre el 80,0% de una y el 80,1% de la siguiente no hay nada.
     * Un valor de 80,04% no cae en ninguna.
     */
    @Test
    void detectaHuecosEntreBandas() {
        Escala conHueco = new Escala(Metrica.FCR, n("3.5"), List.of(
                new Banda(null, n("80.0"), n("0")),
                new Banda(n("80.1"), null, n("3.5"))));

        List<Violacion> violaciones = validador.validar(PliegoDeEjemplo.conUnaSolaEscala(conHueco));

        assertThat(violaciones).hasSize(1);
        assertThat(violaciones.getFirst().tipo()).isEqualTo(Violacion.Tipo.HUECO);
        assertThat(violaciones.getFirst().mensaje()).contains("80.0", "80.1");
    }

    @Test
    void exigeQueLaEscalaCubraPorArriba() {
        Escala cerradaArriba = new Escala(Metrica.FCR, n("3.5"), List.of(
                new Banda(null, n("80"), n("0")),
                new Banda(n("80"), n("100"), n("3.5"))));

        assertThat(validador.validar(PliegoDeEjemplo.conUnaSolaEscala(cerradaArriba)))
                .extracting(Violacion::tipo)
                .containsExactly(Violacion.Tipo.SIN_EXTREMO_ABIERTO);
    }

    @Test
    void exigeQueLaEscalaCubraPorAbajo() {
        Escala cerradaAbajo = new Escala(Metrica.TMO, n("3"), List.of(
                new Banda(n("0"), n("640"), n("3")),
                new Banda(n("640"), null, n("0"))));

        assertThat(validador.validar(PliegoDeEjemplo.conUnaSolaEscala(cerradaAbajo)))
                .extracting(Violacion::tipo)
                .containsExactly(Violacion.Tipo.SIN_EXTREMO_ABIERTO);
    }

    @Test
    void elPesoTieneQueCoincidirConLaMejorBanda() {
        Escala pesoMentiroso = new Escala(Metrica.NPS, n("3.5"), List.of(
                new Banda(null, n("50"), n("0")),
                new Banda(n("50"), null, n("2.0"))));

        assertThat(validador.validar(PliegoDeEjemplo.conUnaSolaEscala(pesoMentiroso)))
                .extracting(Violacion::tipo)
                .containsExactly(Violacion.Tipo.PESO_NO_COINCIDE);
    }

    @Test
    void unaMetricaNoPuedeAparecerDosVeces() {
        var pliego = new com.lautaro.multimetrico.pliego.domain.Pliego(
                java.time.YearMonth.of(2026, 8), "CONVERGENTES", List.of(
                        PliegoDeEjemplo.escalaSana(Metrica.NPS, "3.5"),
                        PliegoDeEjemplo.escalaSana(Metrica.NPS, "3.5")));

        assertThat(validador.validar(pliego))
                .extracting(Violacion::tipo)
                .contains(Violacion.Tipo.METRICA_DUPLICADA);
    }

    /** Un pliego roto de varias formas devuelve todos los problemas, no el primero. */
    @Test
    void informaTodasLasViolacionesDeUnaVez() {
        Escala rota = new Escala(Metrica.NPS, n("9"), List.of(
                new Banda(n("0"), n("50"), n("0")),
                new Banda(n("60"), null, n("3.5"))));

        assertThat(validador.validar(PliegoDeEjemplo.conUnaSolaEscala(rota)))
                .extracting(Violacion::tipo)
                .containsExactlyInAnyOrder(
                        Violacion.Tipo.SIN_EXTREMO_ABIERTO,
                        Violacion.Tipo.HUECO,
                        Violacion.Tipo.PESO_NO_COINCIDE);
    }
}
