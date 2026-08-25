package com.lautaro.multimetrico.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.pliego.PliegoRepository;
import com.lautaro.multimetrico.pliego.domain.Banda;
import com.lautaro.multimetrico.pliego.domain.Escala;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import com.lautaro.multimetrico.pliego.domain.Pliego;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * El pliego tiene que volver de la base exactamente como entró: si no, el mes
 * deja de ser reproducible, que es el defecto número uno del PLAN.
 */
@SpringBootTest
class PliegoEnBaseTest {

    private static final String CAMPANIA = "PERSISTENCIA";

    @Autowired
    private PliegoRepository repositorio;

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
        repositorio.guardar(pliego(periodo));

        Escala fcr = repositorio.buscar(periodo, CAMPANIA).orElseThrow().escalaDe(Metrica.FCR).orElseThrow();

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
        repositorio.guardar(pliego(periodo));

        Escala fcr = repositorio.buscar(periodo, CAMPANIA).orElseThrow().escalaDe(Metrica.FCR).orElseThrow();
        Escala tmo = repositorio.buscar(periodo, CAMPANIA).orElseThrow().escalaDe(Metrica.TMO).orElseThrow();

        assertThat(fcr.ordenadas().getFirst().rango()).isEqualTo("[-∞, 80.1)");
        assertThat(tmo.ordenadas().get(1).rango()).isEqualTo("[600, 700)");
    }

    /**
     * El caso que el PLAN nombra: un FCR de 80,04% se muestra redondeado como
     * "80,0%" pero se compara completo. Si la base redondeara el borde de la
     * banda, este valor caería del otro lado.
     */
    @Test
    void unValorSobreElBordeSigueCayendoEnLaMismaBandaDespuesDeGuardarElPliego() {
        YearMonth periodo = YearMonth.of(2041, 3);
        repositorio.guardar(pliego(periodo));

        Pliego guardado = repositorio.buscar(periodo, CAMPANIA).orElseThrow();

        assertThat(guardado.evaluar(java.util.Map.of(Metrica.FCR, n("80.04"))).alcances())
                .hasEntrySatisfying(Metrica.FCR, alcance ->
                        assertThat(alcance.banda().rango()).isEqualTo("[-∞, 80.1)"));
    }

    /** El orden de las escalas y el de las bandas dentro de cada escala se conservan. */
    @Test
    void lasEscalasYLasBandasVuelvenEnElOrdenEnQueSeCargaron() {
        YearMonth periodo = YearMonth.of(2041, 4);
        repositorio.guardar(pliego(periodo));

        Pliego guardado = repositorio.buscar(periodo, CAMPANIA).orElseThrow();

        assertThat(guardado.escalas()).extracting(Escala::metrica).containsExactly(Metrica.FCR, Metrica.TMO);
        assertThat(guardado.escalaDe(Metrica.TMO).orElseThrow().bandas())
                .extracting(banda -> banda.resultado().toPlainString())
                .containsExactly("3", "1", "0");
    }

    /**
     * La campaña se busca normalizada y no según la collation del motor: MySQL
     * compara sin distinguir mayúsculas y H2 sí las distingue, así que sin la
     * columna de clave este test pasaría en una base y fallaría en la otra.
     */
    @Test
    void laCampaniaSeBuscaSinDistinguirMayusculasNiEspaciosAlBorde() {
        YearMonth periodo = YearMonth.of(2041, 5);
        repositorio.guardar(pliego(periodo));

        assertThat(repositorio.buscar(periodo, "  persistencia  ")).isPresent();
        assertThat(repositorio.existe(periodo, "Persistencia")).isTrue();
        assertThat(repositorio.existe(periodo, "OTRA")).isFalse();
    }

    /** El alcance máximo es la suma de los pesos, y los pesos también vuelven enteros. */
    @Test
    void elAlcanceMaximoSobreviveAlViajeALaBase() {
        YearMonth periodo = YearMonth.of(2041, 6);
        repositorio.guardar(pliego(periodo));

        assertThat(repositorio.buscar(periodo, CAMPANIA).orElseThrow().alcanceMaximo())
                .isEqualByComparingTo("7");
    }
}
