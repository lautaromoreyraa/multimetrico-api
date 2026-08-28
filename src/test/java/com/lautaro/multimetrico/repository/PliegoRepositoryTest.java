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

/**
 * El pliego tiene que volver de la base exactamente como entró: si no, el mes
 * deja de ser reproducible, que es el defecto número uno del PLAN.
 *
 * <p>Con el modelo único, {@link Pliego} <em>es</em> la entidad: lo que antes se
 * verificaba sobre la traducción de ida y vuelta ahora se verifica sobre el
 * mismo objeto releído. Lo que se prueba no cambió —que los extremos abiertos
 * sigan abiertos, que los decimales no se redondeen, que el orden se conserve—,
 * cambió quién hace el trabajo.
 *
 * <p>Las lecturas traen escalas y bandas en la misma consulta gracias al
 * {@code @EntityGraph} del repositorio. Sin él, con {@code open-in-view: false},
 * cada uno de estos tests fallaría con {@code LazyInitializationException} apenas
 * toca la lista: <b>estos tests son también los que defienden ese grafo</b>.
 */
@SpringBootTest
class PliegoRepositoryTest {

    private static final String CAMPANIA = "PERSISTENCIA";

    @Autowired
    private PliegoRepository repositorio;

    /**
     * Evaluar dejó de ser un método del pliego y pasó a la capa de servicio, así
     * que el test del borde lo necesita inyectado.
     */
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

    /**
     * Los extremos abiertos son {@code null} y siguen siéndolo al volver.
     *
     * <p>Guardarlos como un centinela —un cero, un número enorme— convertiría "no
     * tiene piso" en "tiene un piso muy bajo", y la banda dejaría de atrapar los
     * valores que se supone que atrapa.
     */
    @Test
    void losExtremosAbiertosVuelvenAbiertos() {
        YearMonth periodo = YearMonth.of(2041, 1);
        repositorio.save(pliego(periodo));

        Escala fcr = releer(periodo).escalaDe(Metrica.FCR).orElseThrow();

        assertThat(fcr.ordenadas().getFirst().abiertaAbajo()).isTrue();
        assertThat(fcr.ordenadas().getLast().abiertaArriba()).isTrue();
    }

    /**
     * El rango se muestra con los números que alguien escribió, no con los seis
     * decimales de la columna. Es el texto que ve quien carga el pliego y quien
     * lee su alcance.
     */
    @Test
    void elRangoDeUnaBandaSeLeeIgualDespuesDeIrYVolverDeLaBase() {
        YearMonth periodo = YearMonth.of(2041, 2);
        repositorio.save(pliego(periodo));

        Escala fcr = releer(periodo).escalaDe(Metrica.FCR).orElseThrow();
        Escala tmo = releer(periodo).escalaDe(Metrica.TMO).orElseThrow();

        assertThat(fcr.ordenadas().getFirst().rango()).isEqualTo("[-∞, 80.1)");
        assertThat(tmo.ordenadas().get(1).rango()).isEqualTo("[600, 700)");
    }

    /**
     * El caso que el PLAN nombra: un FCR de 80,04% se muestra redondeado como
     * "80,0%" pero se compara completo. Si la base redondeara el borde de la
     * banda, este valor caería del otro lado.
     *
     * <p>La evaluación la hace ahora {@code PliegoService} y el alcance devuelve
     * el rango <b>ya como texto</b>, así que se compara contra el string en vez de
     * pedirle el rango a un objeto banda.
     */
    @Test
    void unValorSobreElBordeSigueCayendoEnLaMismaBandaDespuesDeGuardarElPliego() {
        YearMonth periodo = YearMonth.of(2041, 3);
        repositorio.save(pliego(periodo));

        Pliego guardado = releer(periodo);

        assertThat(pliegos.evaluar(guardado, Map.of(Metrica.FCR, n("80.04"))).alcances())
                .hasEntrySatisfying(Metrica.FCR, alcance ->
                        assertThat(alcance.banda()).isEqualTo("[-∞, 80.1)"));
    }

    /** El orden de las escalas y el de las bandas dentro de cada escala se conservan. */
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

    /**
     * La campaña se busca normalizada y no según la collation del motor: MySQL
     * compara sin distinguir mayúsculas y H2 sí las distingue, así que sin la
     * columna de clave este test pasaría en una base y fallaría en la otra.
     *
     * <p>Quien normaliza cambió de lugar —antes era el adaptador, ahora es
     * {@link Pliego#clave} en el llamador—, pero la garantía es la misma: dos
     * escrituras distintas de la misma campaña encuentran el mismo pliego.
     */
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

    /** El alcance máximo es la suma de los pesos, y los pesos también vuelven enteros. */
    @Test
    void elAlcanceMaximoSobreviveAlViajeALaBase() {
        YearMonth periodo = YearMonth.of(2041, 6);
        repositorio.save(pliego(periodo));

        assertThat(releer(periodo).alcanceMaximo()).isEqualByComparingTo("7");
    }

    /**
     * El listado también trae las escalas cargadas.
     *
     * <p>No estaba en la versión anterior y se agrega acá porque
     * {@code findAllByOrderBy...} es la otra consulta que lleva
     * {@code @EntityGraph}: es la que alimenta {@code GET /api/pliegos}, y si el
     * grafo se cayera de esa firma el endpoint reventaría en el mapper sin que
     * ningún otro test lo notara.
     */
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
