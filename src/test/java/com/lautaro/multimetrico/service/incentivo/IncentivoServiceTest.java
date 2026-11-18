package com.lautaro.multimetrico.service.incentivo;

import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.incentivo.CargaDeIncentivos;
import com.lautaro.multimetrico.dto.incentivo.FilaDeIncentivo;
import com.lautaro.multimetrico.dto.perfil.PerfilDeIncentivos;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;
import com.lautaro.multimetrico.fixture.PlanillaSintetica;
import com.lautaro.multimetrico.service.padron.PadronService;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IncentivoServiceTest {

    private static final AtomicInteger MES = new AtomicInteger();

    private static final Asesor FLAVIA = new Asesor("q100", "AGUIRRE FLAVIA YAMILA", "PERALTA SILVIA");
    private static final Asesor HECTOR = new Asesor("q200", "MIÑO HECTOR BAUTISTA", "PERALTA SILVIA");
    private static final Asesor ANA = new Asesor("q300", "SOSA ANA MARIA", "LEDESMA JORGE");
    private static final Asesor ANABELA = new Asesor("q400", "SOSA ANABELA ROSA", "LEDESMA JORGE");

    @Autowired
    private IncentivoService servicio;

    @Autowired
    private PadronService padrones;

    private YearMonth periodo;

    @BeforeEach
    void cargarPadron() {
        periodo = YearMonth.of(2060, 1).plusMonths(MES.getAndIncrement());
        padrones.cargar(new Padron(periodo, List.of(
                copia(FLAVIA), copia(HECTOR), copia(ANA), copia(ANABELA))));
    }

    private static Asesor copia(Asesor asesor) {
        return new Asesor(asesor.legajo(), asesor.nombre(), asesor.lider());
    }

    private PerfilDeIncentivos perfil() {
        return new PerfilDeIncentivos("Datos", 1, "ASESOR", "IMPORTE", SeparadorDecimal.COMA);
    }

    private CargaDeIncentivos cargar(byte[] archivo) {
        return servicio.cargar(periodo, perfil(), archivo, "incentivos.xlsx");
    }

    @Test
    void cargaLosImportesYLosDevuelveEnLaLista() {
        byte[] archivo = PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "IMPORTE")
                .filaConValor("AGUIRRE FLAVIA YAMILA", 152400.0, null)
                .filaConValor("MIÑO HECTOR BAUTISTA", 138900.5, null)
                .bytes();

        CargaDeIncentivos carga = cargar(archivo);

        assertThat(carga.filasLeidas()).isEqualTo(2);
        assertThat(carga.importadas()).isEqualTo(2);
        assertThat(carga.rechazadas()).isEmpty();

        var equipo = servicio.delEquipo(periodo, "PERALTA SILVIA");
        assertThat(equipo.filas()).hasSize(2);
        assertThat(importeDe(equipo.filas(), "q100")).isEqualByComparingTo("152400");
        assertThat(importeDe(equipo.filas(), "q200")).isEqualByComparingTo("138900.5");
    }

    @Test
    void quienNoVinoEnElArchivoApareceConElImporteVacio() {
        cargar(PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "IMPORTE")
                .filaConValor("AGUIRRE FLAVIA YAMILA", 100000.0, null)
                .bytes());

        var equipo = servicio.delEquipo(periodo, "PERALTA SILVIA");

        assertThat(equipo.asesores()).isEqualTo(2);
        assertThat(equipo.sinImporte()).isEqualTo(1);
        assertThat(importeDe(equipo.filas(), "q200")).isNull();
    }

    @Test
    void elTotalSumaSoloLoQueVino() {
        cargar(PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "IMPORTE")
                .filaConValor("AGUIRRE FLAVIA YAMILA", 100000.0, null)
                .bytes());

        var equipo = servicio.delEquipo(periodo, "PERALTA SILVIA");

        assertThat(equipo.total()).isEqualByComparingTo("100000");
        assertThat(equipo.sinImporte()).isEqualTo(1);
    }

    @Test
    void unNombreQueNoEstaEnElPadronSeRechazaConSuFila() {
        CargaDeIncentivos carga = cargar(PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "IMPORTE")
                .filaConValor("AGUIRRE FLAVIA YAMILA", 100000.0, null)
                .filaConValor("PEREZ QUIENSABE", 90000.0, null)
                .bytes());

        assertThat(carga.filasLeidas()).isEqualTo(2);
        assertThat(carga.importadas()).isEqualTo(1);
        assertThat(carga.cuadra()).isTrue();
        assertThat(carga.rechazadas()).singleElement().satisfies(fila -> {
            assertThat(fila.textoAsesor()).isEqualTo("PEREZ QUIENSABE");
            assertThat(fila.motivo()).contains("no se parece a nadie");
        });
    }

    @Test
    void unNombreAmbiguoSeRechazaConLosCandidatos() {
        CargaDeIncentivos carga = cargar(PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "IMPORTE")
                .filaConValor("SOSA ANA", 120000.0, null)
                .bytes());

        assertThat(carga.importadas()).isZero();
        assertThat(carga.rechazadas()).singleElement().satisfies(fila -> {
            assertThat(fila.motivo()).contains("más de un asesor");
            assertThat(fila.motivo()).contains("SOSA ANA MARIA");
            assertThat(fila.motivo()).contains("SOSA ANABELA ROSA");
        });
    }

    @Test
    void volverACargarPisaElImporteAnterior() {
        cargar(PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "IMPORTE")
                .filaConValor("AGUIRRE FLAVIA YAMILA", 100000.0, null)
                .bytes());

        cargar(PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "IMPORTE")
                .filaConValor("AGUIRRE FLAVIA YAMILA", 175500.0, null)
                .bytes());

        var equipo = servicio.delEquipo(periodo, "PERALTA SILVIA");
        assertThat(importeDe(equipo.filas(), "q100")).isEqualByComparingTo("175500");
    }

    @Test
    void sinLiderTraeLaOperacionEntera() {
        cargar(PlanillaSintetica.hoja("Datos").fila("ASESOR", "IMPORTE").bytes());

        assertThat(servicio.delEquipo(periodo, null).asesores()).isEqualTo(4);
    }

    private static BigDecimal importeDe(List<FilaDeIncentivo> filas, String legajo) {
        return filas.stream()
                .filter(fila -> fila.asesor().legajo().equals(legajo))
                .findFirst()
                .orElseThrow()
                .importe();
    }
}
