package com.lautaro.multimetrico.multimetrico;

import static com.lautaro.multimetrico.multimetrico.EscenarioDeEjemplo.medicion;
import static com.lautaro.multimetrico.multimetrico.EscenarioDeEjemplo.padron;
import static com.lautaro.multimetrico.multimetrico.EscenarioDeEjemplo.pliego;
import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.medicion.domain.Medicion;
import com.lautaro.multimetrico.medicion.domain.MedicionesVigentes;
import com.lautaro.multimetrico.multimetrico.domain.CeldaDeMultimetrico;
import com.lautaro.multimetrico.multimetrico.domain.FilaDeMultimetrico;
import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import com.lautaro.multimetrico.pliego.domain.Pliego;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class FilaDeMultimetricoTest {

    private static final YearMonth ENERO = YearMonth.of(2027, 1);

    private final Pliego pliego = pliego(ENERO);
    private final Asesor flavia = padron(ENERO).porLegajo("u100").orElseThrow();

    private FilaDeMultimetrico fila(List<Medicion> historia) {
        return FilaDeMultimetrico.armar(pliego, flavia, MedicionesVigentes.de(historia));
    }

    /**
     * El total suma lo que se pudo calcular y las faltantes viajan aparte. Un 3
     * con dos métricas sin cargar no es lo mismo que un 3 con todo cargado.
     */
    @Test
    void elTotalSumaSoloLoCalculableYLasFaltantesViajanAparte() {
        FilaDeMultimetrico fila = fila(List.of(
                medicion(ENERO, "u100", Metrica.FCR, "78.2", 22),
                medicion(ENERO, "u100", Metrica.TMO, "612", 19)));

        assertThat(fila.alcanceTotal()).isEqualByComparingTo("5");
        assertThat(fila.faltantes()).containsExactly(Metrica.NPS);
        assertThat(fila.completa()).isFalse();
    }

    /** Nunca se rellena con cero: el faltante no baja el total, lo deja incompleto. */
    @Test
    void unAsesorSinNingunaMedicionApareceIgualConTodoFaltante() {
        FilaDeMultimetrico fila = fila(List.of());

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
        FilaDeMultimetrico fila = fila(List.of(
                medicion(ENERO, "u100", Metrica.FCR, "78.2", 22),
                medicion(ENERO, "u100", Metrica.TMO, "612", 19),
                medicion(ENERO, "u100", Metrica.NPS, "55.4", 22)));

        assertThat(fila.cortesPorMetrica())
                .containsEntry(Metrica.FCR, ENERO.atDay(22))
                .containsEntry(Metrica.TMO, ENERO.atDay(19))
                .containsEntry(Metrica.NPS, ENERO.atDay(22));

        assertThat(fila.corteMasViejo()).contains(ENERO.atDay(19));
        assertThat(fila.corteMasNuevo()).contains(ENERO.atDay(22));
    }

    /** Se evalúa la vigente, no la última cargada ni la primera. */
    @Test
    void laCeldaSeArmaConLaMedicionVigenteYNoConLaHistoriaEntera() {
        FilaDeMultimetrico fila = fila(List.of(
                medicion(ENERO, "u100", Metrica.FCR, "70.5", 15),
                medicion(ENERO, "u100", Metrica.FCR, "78.2", 22)));

        CeldaDeMultimetrico celda = fila.celdas().getFirst();

        assertThat(celda.metrica()).isEqualTo(Metrica.FCR);
        assertThat(celda.alcance().valor()).isEqualByComparingTo("78.2");
        assertThat(celda.alcance().banda().rango()).isEqualTo("[75, +∞)");
        assertThat(celda.actualizadoAl()).isEqualTo(ENERO.atDay(22));
    }

    @Test
    void cadaCeldaTraeElTramoDeArribaSalvoCuandoYaEstaArriba() {
        FilaDeMultimetrico fila = fila(List.of(
                medicion(ENERO, "u100", Metrica.FCR, "78.2", 22),
                medicion(ENERO, "u100", Metrica.TMO, "612", 19)));

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
