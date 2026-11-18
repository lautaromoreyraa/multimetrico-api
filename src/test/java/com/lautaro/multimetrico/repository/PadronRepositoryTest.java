package com.lautaro.multimetrico.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.domain.AliasDeNombre;
import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.service.padron.PadronService;
import com.lautaro.multimetrico.util.NombreNormalizado;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PadronRepositoryTest {

    @Autowired
    private PadronRepository padrones;

    @Autowired
    private AliasRepository alias;

    @Autowired
    private PadronService servicio;

    @Test
    void cargarElPadronDeUnPeriodoDosVecesLoReemplazaEnVezDeAcumularlo() {
        YearMonth periodo = YearMonth.of(2042, 1);

        servicio.cargar(new Padron(periodo, List.of(
                new Asesor("q100", "AGUIRRE FLAVIA YAMILA", "PERALTA SILVIA"),
                new Asesor("q200", "MIÑO HECTOR BAUTISTA", "PERALTA SILVIA"))));

        servicio.cargar(new Padron(periodo, List.of(
                new Asesor("q100", "AGUIRRE FLAVIA YAMILA", "LEDESMA JORGE"))));

        Padron guardado = padrones.findByPeriodo(periodo).orElseThrow();

        assertThat(guardado.tamano()).isEqualTo(1);
        assertThat(guardado.lideres()).containsExactly("LEDESMA JORGE");
    }

    @Test
    void losNombresConEnieYAcentosVuelvenIgualDeLaBase() {
        YearMonth periodo = YearMonth.of(2042, 2);

        servicio.cargar(new Padron(periodo, List.of(
                new Asesor("q300", "MIÑO HÉCTOR BAUTISTA", "PERALTA SILVIA"))));

        assertThat(padrones.findByPeriodo(periodo).orElseThrow().porLegajo("q300")).get()
                .satisfies(asesor -> assertThat(asesor.nombre()).isEqualTo("MIÑO HÉCTOR BAUTISTA"));
    }

    @Test
    void losPeriodosSalenDelMasNuevoAlMasViejo() {
        servicio.cargar(new Padron(YearMonth.of(2043, 3),
                List.of(new Asesor("q400", "UNA PERSONA", "UN LIDER"))));
        servicio.cargar(new Padron(YearMonth.of(2043, 11),
                List.of(new Asesor("q400", "UNA PERSONA", "UN LIDER"))));

        assertThat(padrones.findAllByOrderByPeriodoDesc())
                .extracting(Padron::periodo)
                .containsSubsequence(YearMonth.of(2043, 11), YearMonth.of(2043, 3));
    }

    @Test
    void elPadronVuelveConSusAsesoresCargadosYNoPerezosos() {
        YearMonth periodo = YearMonth.of(2042, 3);

        servicio.cargar(new Padron(periodo, List.of(
                new Asesor("q800", "UNA PERSONA", "UN LIDER"),
                new Asesor("q801", "OTRA PERSONA", "UN LIDER"))));

        assertThat(padrones.findByPeriodo(periodo).orElseThrow().asesores()).hasSize(2);
    }

    @Test
    void unAliasConfirmadoSobreviveYNoDependeDelPeriodo() {
        YearMonth periodo = YearMonth.of(2042, 4);
        servicio.cargar(new Padron(periodo, List.of(
                new Asesor("q500", "AGUIRRE FLAVIA YAMILA", "PERALTA SILVIA"))));

        servicio.confirmarAlias(periodo, "AGUIRRE FLAVIA Y", "q500");

        assertThat(alias.findByNombre(NombreNormalizado.de("AGUIRRE FLAVIA Y").valor()))
                .get()
                .extracting(AliasDeNombre::legajo)
                .isEqualTo("q500");
        assertThat(servicio.aliasDe("q500")).contains("AGUIRRE FLAVIA Y");
    }

    @Test
    void volverAConfirmarUnAliasLoReapunta() {
        YearMonth periodo = YearMonth.of(2042, 5);
        servicio.cargar(new Padron(periodo, List.of(
                new Asesor("q600", "SOSA ANA MARIA", "LEDESMA JORGE"),
                new Asesor("q700", "SOSA ANABELA ROSA", "LEDESMA JORGE"))));

        servicio.confirmarAlias(periodo, "SOSA ANA", "q600");
        servicio.confirmarAlias(periodo, "SOSA ANA", "q700");

        assertThat(alias.findByNombre(NombreNormalizado.de("SOSA ANA").valor()))
                .get()
                .extracting(AliasDeNombre::legajo)
                .isEqualTo("q700");
        assertThat(servicio.aliasDe("q600")).doesNotContain("SOSA ANA");
    }
}
