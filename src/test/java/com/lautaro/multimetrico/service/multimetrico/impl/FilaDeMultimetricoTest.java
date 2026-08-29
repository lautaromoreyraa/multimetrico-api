package com.lautaro.multimetrico.service.multimetrico.impl;

import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.padron;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.pliego;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.vigentes;
import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.medicion.MedicionesVigentes;
import com.lautaro.multimetrico.dto.multimetrico.CeldaDeMultimetrico;
import com.lautaro.multimetrico.dto.multimetrico.FilaDeMultimetrico;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.mapper.impl.PadronMapperImpl;
import com.lautaro.multimetrico.service.pliego.impl.PliegoServiceImpl;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

/**
 * El armado de la fila de un asesor: una función pura de pliego, persona y
 * mediciones vigentes.
 *
 * <p>Las dos dependencias que van en {@code null} son las que buscan en la base
 * —el padrón y las mediciones—, y son justamente las que este cálculo no usa:
 * quien elige el pliego del período y busca las vigentes es el método público que
 * lo llama. Que se pueda construir así <b>es</b> lo que se está probando.
 *
 * <p>Las vigentes llegan ya resueltas. Cuál de dos cargas gana es una regla de
 * {@code MedicionService} y tiene sus propios tests; mezclarla acá haría que este
 * test fallara por dos motivos distintos.
 */
class FilaDeMultimetricoTest {

    private static final YearMonth ENERO = YearMonth.of(2027, 1);

    private final MultimetricoServiceImpl multimetrico = new MultimetricoServiceImpl(
            new PliegoServiceImpl(null, null), null, null, new PadronMapperImpl());

    private final Pliego pliego = pliego(ENERO);
    private final Asesor flavia = padron(ENERO).porLegajo("u100").orElseThrow();

    private FilaDeMultimetrico fila(MedicionesVigentes vigentes) {
        return multimetrico.armarFila(pliego, flavia, vigentes);
    }

    /**
     * El total suma lo que se pudo calcular y las faltantes viajan aparte. Un 3
     * con dos métricas sin cargar no es lo mismo que un 3 con todo cargado.
     */
    @Test
    void elTotalSumaSoloLoCalculableYLasFaltantesViajanAparte() {
        FilaDeMultimetrico fila = fila(vigentes(ENERO,
                Metrica.FCR, "78.2", 22,
                Metrica.TMO, "612", 19));

        assertThat(fila.alcanceTotal()).isEqualByComparingTo("5");
        assertThat(fila.faltantes()).containsExactly(Metrica.NPS);
        assertThat(fila.completa()).isFalse();
    }

    /** Nunca se rellena con cero: el faltante no baja el total, lo deja incompleto. */
    @Test
    void unAsesorSinNingunaMedicionApareceIgualConTodoFaltante() {
        FilaDeMultimetrico fila = fila(MedicionesVigentes.ninguna());

        assertThat(fila.asesor().legajo()).isEqualTo("u100");
        assertThat(fila.celdas()).isEmpty();
        assertThat(fila.faltantes()).containsExactlyInAnyOrder(Metrica.FCR, Metrica.TMO, Metrica.NPS);
        assertThat(fila.alcanceTotal()).isEqualByComparingTo("0");
        assertThat(fila.completa()).isFalse();
    }

    /**
     * Lo que la planilla pierde: cada métrica se actualizó un día distinto y el
     * multimétrico las junta como si fueran la misma foto.
     */
    @Test
    void cadaMetricaLlevaLaFechaDeCorteDeLaMedicionQueSeUso() {
        FilaDeMultimetrico fila = fila(vigentes(ENERO,
                Metrica.FCR, "78.2", 22,
                Metrica.TMO, "612", 19,
                Metrica.NPS, "55.4", 22));

        assertThat(fila.cortesPorMetrica())
                .containsEntry(Metrica.FCR, ENERO.atDay(22))
                .containsEntry(Metrica.TMO, ENERO.atDay(19))
                .containsEntry(Metrica.NPS, ENERO.atDay(22));

        assertThat(fila.corteMasViejo()).isEqualTo(ENERO.atDay(19));
        assertThat(fila.corteMasNuevo()).isEqualTo(ENERO.atDay(22));
    }

    /** La celda se arma con el valor vigente, con su banda y con su corte. */
    @Test
    void laCeldaTraeElValorConSuBandaYSuCorte() {
        FilaDeMultimetrico fila = fila(vigentes(ENERO, Metrica.FCR, "78.2", 22));

        CeldaDeMultimetrico celda = fila.celdas().getFirst();

        assertThat(celda.metrica()).isEqualTo(Metrica.FCR);
        assertThat(celda.alcance().valor()).isEqualByComparingTo("78.2");
        assertThat(celda.alcance().banda()).isEqualTo("[75, +∞)");
        assertThat(celda.actualizadoAl()).isEqualTo(ENERO.atDay(22));
    }

    @Test
    void cadaCeldaTraeElTramoDeArribaSalvoCuandoYaEstaArriba() {
        FilaDeMultimetrico fila = fila(vigentes(ENERO,
                Metrica.FCR, "78.2", 22,
                Metrica.TMO, "612", 19));

        CeldaDeMultimetrico fcr = fila.celdas().stream()
                .filter(celda -> celda.metrica() == Metrica.FCR).findFirst().orElseThrow();
        CeldaDeMultimetrico tmo = fila.celdas().stream()
                .filter(celda -> celda.metrica() == Metrica.TMO).findFirst().orElseThrow();

        assertThat(fcr.tramoDeArriba()).isEmpty();
        assertThat(tmo.tramoDeArriba()).get()
                .extracting(tramo -> tramo.objetivo().toPlainString())
                .isEqualTo("600");
    }
}
