package com.lautaro.multimetrico.service.multimetrico;

import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.CAMPANIA;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.medicion;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.mediciones;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.n;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.padron;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.pliego;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lautaro.multimetrico.domain.Banda;
import com.lautaro.multimetrico.domain.Escala;
import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.multimetrico.FilaDeMultimetrico;
import com.lautaro.multimetrico.dto.multimetrico.Multimetrico;
import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.exception.RecursoNoEncontradoException;
import com.lautaro.multimetrico.service.medicion.MedicionService;
import com.lautaro.multimetrico.service.padron.PadronService;
import com.lautaro.multimetrico.service.pliego.PliegoService;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MultimetricoServiceTest {

    @org.junit.jupiter.api.BeforeEach
    void actuarComoAdmin() {
        com.lautaro.multimetrico.fixture.Identidades.actuarComo(
                com.lautaro.multimetrico.fixture.Identidades.admin());
    }

    @AfterEach
    void olvidarIdentidad() {
        com.lautaro.multimetrico.fixture.Identidades.olvidar();
    }

    @Autowired
    private MultimetricoService multimetricos;

    @Autowired
    private PliegoService pliegos;

    @Autowired
    private PadronService padrones;

    @Autowired
    private MedicionService medicionesDelMes;

    private void escenarioCompleto(YearMonth periodo) {
        pliegos.cargar(pliego(periodo));
        padrones.cargar(padron(periodo));
        medicionesDelMes.registrar(mediciones(periodo));
    }

    @Test
    void armaUnaFilaPorCadaAsesorDelEquipoAunqueNoTengaMediciones() {
        YearMonth periodo = YearMonth.of(2029, 2);
        pliegos.cargar(pliego(periodo));
        padrones.cargar(padron(periodo));
        medicionesDelMes.registrar(List.of(medicion(periodo, "u100", Metrica.FCR, "78.2", 22)));

        Multimetrico multimetrico = multimetricos.delEquipo(periodo, "PERALTA SILVIA", CAMPANIA);

        assertThat(multimetrico.filas())
                .extracting(fila -> fila.asesor().legajo())
                .containsExactly("u100", "u200");

        FilaDeMultimetrico sinDatos = multimetrico.filas().get(1);
        assertThat(sinDatos.celdas()).isEmpty();
        assertThat(sinDatos.faltantes()).containsExactlyInAnyOrder(Metrica.FCR, Metrica.TMO, Metrica.NPS);
    }

    @Test
    void elEquipoSaleFiltradoPorSuLiderYConElAlcanceMaximoDelPliego() {
        YearMonth periodo = YearMonth.of(2029, 3);
        escenarioCompleto(periodo);

        Multimetrico deSilvia = multimetricos.delEquipo(periodo, "PERALTA SILVIA", CAMPANIA);
        Multimetrico deJorge = multimetricos.delEquipo(periodo, "LEDESMA JORGE", CAMPANIA);

        assertThat(deSilvia.resumen().asesores()).isEqualTo(2);
        assertThat(deJorge.resumen().asesores()).isEqualTo(1);
        assertThat(deSilvia.alcanceMaximo()).isEqualByComparingTo("10.5");

        assertThat(deSilvia.filas().getFirst().alcanceTotal()).isEqualByComparingTo("8.5");
        assertThat(deSilvia.filas().getFirst().completa()).isTrue();
        assertThat(deSilvia.resumen().completos()).isEqualTo(1);
        assertThat(deSilvia.resumen().conFaltantes()).isEqualTo(1);
    }

    @Test
    void sinLiderDevuelveLaOperacionEntera() {
        YearMonth periodo = YearMonth.of(2029, 4);
        escenarioCompleto(periodo);

        assertThat(multimetricos.delEquipo(periodo, null, CAMPANIA).resumen().asesores()).isEqualTo(3);
    }

    @Test
    void avisaCuandoElEquipoTieneMasDeUnCorteEnLaMismaMetrica() {
        YearMonth periodo = YearMonth.of(2029, 5);
        pliegos.cargar(pliego(periodo));
        padrones.cargar(padron(periodo));
        medicionesDelMes.registrar(List.of(
                medicion(periodo, "u100", Metrica.FCR, "78.2", 22),
                medicion(periodo, "u200", Metrica.FCR, "61.0", 15)));

        Multimetrico multimetrico = multimetricos.delEquipo(periodo, "PERALTA SILVIA", CAMPANIA);

        assertThat(multimetrico.resumen().cortesPorMetrica().get(Metrica.FCR))
                .containsExactly(periodo.atDay(15), periodo.atDay(22));
        assertThat(multimetrico.resumen().actualizadoDesde()).isEqualTo(periodo.atDay(15));
    }

    @Test
    void evaluaConElPliegoDelPeriodoYNoConElVigente() {
        YearMonth enero = YearMonth.of(2029, 6);
        YearMonth febrero = YearMonth.of(2029, 7);

        pliegos.cargar(pliego(enero));
        pliegos.cargar(new Pliego(febrero, CAMPANIA, List.of(
                new Escala(Metrica.FCR, n("4"), List.of(
                        new Banda(null, n("60"), n("0")),
                        new Banda(n("60"), n("80"), n("2")),
                        new Banda(n("80"), null, n("4")))))));

        for (YearMonth periodo : List.of(enero, febrero)) {
            padrones.cargar(padron(periodo));
            medicionesDelMes.registrar(List.of(medicion(periodo, "u100", Metrica.FCR, "78.2", 22)));
        }

        assertThat(multimetricos.deUnAsesor(enero, "u100", CAMPANIA).celdas().getFirst()
                .alcance().resultado()).isEqualByComparingTo("4");
        assertThat(multimetricos.deUnAsesor(febrero, "u100", CAMPANIA).celdas().getFirst()
                .alcance().resultado()).isEqualByComparingTo("2");
    }

    @Test
    void sinPliegoDelPeriodoDiceQueLoQueFaltaEsElPliego() {
        assertThatThrownBy(() -> multimetricos.delEquipo(YearMonth.of(2029, 8), "PERALTA SILVIA", CAMPANIA))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("No hay pliego cargado para 2029-08");
    }

    @Test
    void sinPadronDelPeriodoDiceQueLoQueFaltaEsElPadron() {
        YearMonth periodo = YearMonth.of(2029, 9);
        pliegos.cargar(pliego(periodo));

        assertThatThrownBy(() -> multimetricos.delEquipo(periodo, "PERALTA SILVIA", CAMPANIA))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("No hay padrón cargado para 2029-09");
    }

    @Test
    void unLiderQueNoEstaEnElPadronEsUnErrorYNoUnaTablaVacia() {
        YearMonth periodo = YearMonth.of(2029, 10);
        escenarioCompleto(periodo);

        assertThatThrownBy(() -> multimetricos.delEquipo(periodo, "CABRERA MONICA", CAMPANIA))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no tiene asesores")
                .hasMessageContaining("PERALTA SILVIA");
    }

    @Test
    void laFilaDeUnLegajoQueNoEstaEnElPadronNoSeInventa() {
        YearMonth periodo = YearMonth.of(2029, 11);
        escenarioCompleto(periodo);
        medicionesDelMes.registrar(List.of(medicion(periodo, "u999", Metrica.FCR, "90", 22)));

        assertThatThrownBy(() -> multimetricos.deUnAsesor(periodo, "u999", CAMPANIA))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("u999");
    }

    @Test
    void laFilaDeUnAsesorTraeSuNombreYSuLider() {
        YearMonth periodo = YearMonth.of(2029, 12);
        escenarioCompleto(periodo);

        AsesorResponse asesor = multimetricos.deUnAsesor(periodo, "u100", CAMPANIA).asesor();

        assertThat(asesor.nombre()).isEqualTo("AGUIRRE FLAVIA YAMILA");
        assertThat(asesor.lider()).isEqualTo("PERALTA SILVIA");
    }
}
