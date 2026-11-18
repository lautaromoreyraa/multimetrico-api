package com.lautaro.multimetrico.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.domain.Banda;
import com.lautaro.multimetrico.domain.Escala;
import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.service.pliego.PliegoService;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PliegoRepositoryTest {

    private static final String CAMPANIA = "PERSISTENCIA";

    @Autowired
    private PliegoRepository repositorio;

    @Autowired
    private PliegoService pliegos;

    private static BigDecimal n(String valor) {
        return new BigDecimal(valor);
    }

    private static Pliego pliego(YearMonth periodo) {
        return new Pliego(periodo, CAMPANIA, List.of(
                new Escala(Metrica.FCR, n("4"), List.of(
                        new Banda(null, n("80.1"), n("0")),
                        new Banda(n("80.1"), null, n("4")))),
                new Escala(Metrica.TMO, n("3"), List.of(
                        new Banda(null, n("600"), n("3")),
                        new Banda(n("600"), n("700"), n("1")),
                        new Banda(n("700"), null, n("0"))))));
    }

    private Pliego releer(YearMonth periodo) {
        return repositorio.findByPeriodoAndCampaniaClave(periodo, Pliego.clave(CAMPANIA)).orElseThrow();
    }

    @Test
    void losExtremosAbiertosVuelvenAbiertos() {
        YearMonth periodo = YearMonth.of(2041, 1);
        repositorio.save(pliego(periodo));

        Escala fcr = releer(periodo).escalaDe(Metrica.FCR).orElseThrow();

        assertThat(fcr.ordenadas().getFirst().abiertaAbajo()).isTrue();
        assertThat(fcr.ordenadas().getLast().abiertaArriba()).isTrue();
    }

    @Test
    void elRangoDeUnaBandaSeLeeIgualDespuesDeIrYVolverDeLaBase() {
        YearMonth periodo = YearMonth.of(2041, 2);
        repositorio.save(pliego(periodo));

        Escala fcr = releer(periodo).escalaDe(Metrica.FCR).orElseThrow();
        Escala tmo = releer(periodo).escalaDe(Metrica.TMO).orElseThrow();

        assertThat(fcr.ordenadas().getFirst().rango()).isEqualTo("[-∞, 80.1)");
        assertThat(tmo.ordenadas().get(1).rango()).isEqualTo("[600, 700)");
    }

    @Test
    void unValorSobreElBordeSigueCayendoEnLaMismaBandaDespuesDeGuardarElPliego() {
        YearMonth periodo = YearMonth.of(2041, 3);
        repositorio.save(pliego(periodo));

        Pliego guardado = releer(periodo);

        assertThat(pliegos.evaluar(guardado, Map.of(Metrica.FCR, n("80.04"))).alcances())
                .hasEntrySatisfying(Metrica.FCR, alcance ->
                        assertThat(alcance.banda()).isEqualTo("[-∞, 80.1)"));
    }

    @Test
    void lasEscalasYLasBandasVuelvenEnElOrdenEnQueSeCargaron() {
        YearMonth periodo = YearMonth.of(2041, 4);
        repositorio.save(pliego(periodo));

        Pliego guardado = releer(periodo);

        assertThat(guardado.escalas()).extracting(Escala::metrica).containsExactly(Metrica.FCR, Metrica.TMO);
        assertThat(guardado.escalaDe(Metrica.TMO).orElseThrow().bandas())
                .extracting(banda -> banda.resultado().toPlainString())
                .containsExactly("3", "1", "0");
    }

    @Test
    void laCampaniaSeBuscaSinDistinguirMayusculasNiEspaciosAlBorde() {
        YearMonth periodo = YearMonth.of(2041, 5);
        repositorio.save(pliego(periodo));

        assertThat(repositorio.findByPeriodoAndCampaniaClave(periodo, Pliego.clave("  persistencia  ")))
                .isPresent();
        assertThat(repositorio.existsByPeriodoAndCampaniaClave(periodo, Pliego.clave("Persistencia")))
                .isTrue();
        assertThat(repositorio.existsByPeriodoAndCampaniaClave(periodo, Pliego.clave("OTRA")))
                .isFalse();
    }

    @Test
    void elAlcanceMaximoSobreviveAlViajeALaBase() {
        YearMonth periodo = YearMonth.of(2041, 6);
        repositorio.save(pliego(periodo));

        assertThat(releer(periodo).alcanceMaximo()).isEqualByComparingTo("7");
    }

    @Test
    void elListadoTraeLasEscalasCargadasYNoPerezosas() {
        YearMonth periodo = YearMonth.of(2041, 7);
        repositorio.save(pliego(periodo));

        assertThat(repositorio.findAllByOrderByPeriodoDescCampaniaAsc())
                .filteredOn(p -> p.periodo().equals(periodo))
                .singleElement()
                .satisfies(p -> assertThat(p.escalas()).hasSize(2));
    }
}
