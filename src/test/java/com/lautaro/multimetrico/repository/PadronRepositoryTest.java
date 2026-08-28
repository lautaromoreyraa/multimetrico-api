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

/**
 * Lo que la base tiene que seguir cumpliendo del padrón y de los alias.
 *
 * <p>Dos cosas se mudaron de lugar con la reestructuración y por eso este test
 * inyecta también el servicio:
 *
 * <ul>
 *   <li><b>Reemplazar el padrón</b> —borrar el anterior y escribir el nuevo—
 *       era del adaptador y ahora es {@code PadronServiceImpl.cargar}. Se prueba
 *       ahí, que es donde vive la regla.</li>
 *   <li><b>Confirmar un alias</b> —reapuntar en vez de duplicar— era del
 *       adaptador y ahora es {@code PadronServiceImpl.confirmarAlias}, que además
 *       exige que el legajo exista en el padrón del período. Por eso los tests de
 *       alias cargan un padrón mínimo antes: el alias que apunta a nadie es peor
 *       que no tenerlo, y esa validación también merece quedar cubierta.</li>
 * </ul>
 */
@SpringBootTest
class PadronRepositoryTest {

    @Autowired
    private PadronRepository padrones;

    @Autowired
    private AliasRepository alias;

    @Autowired
    private PadronService servicio;

    /**
     * El padrón <b>sí</b> se reemplaza: es una lista de gente que cambia dentro
     * del mes. Lo que no puede pasar es que las dos versiones queden mezcladas y
     * el equipo aparezca con el doble de asesores, o con alguien que ya se fue.
     */
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

    /** Los apellidos con eñe y con acentos tienen que volver enteros: la base es utf8mb4. */
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

    /**
     * El padrón vuelve con su gente cargada en la misma consulta.
     *
     * <p>No estaba en la versión anterior y se agrega porque ahora hace falta: la
     * entidad viaja hasta el mapper con {@code open-in-view: false}, así que sin
     * el {@code @EntityGraph} del repositorio tocar {@code asesores()} tiraría
     * {@code LazyInitializationException} en el camino principal de todos los
     * endpoints del padrón. <b>Este test es el que defiende ese grafo.</b>
     */
    @Test
    void elPadronVuelveConSusAsesoresCargadosYNoPerezosos() {
        YearMonth periodo = YearMonth.of(2042, 3);

        servicio.cargar(new Padron(periodo, List.of(
                new Asesor("q800", "UNA PERSONA", "UN LIDER"),
                new Asesor("q801", "OTRA PERSONA", "UN LIDER"))));

        assertThat(padrones.findByPeriodo(periodo).orElseThrow().asesores()).hasSize(2);
    }

    /**
     * Los alias son conocimiento que se acumula entre meses y no cuelgan de
     * ningún período: si en marzo alguien confirmó qué legajo es cierto texto mal
     * codificado, en abril el sistema no vuelve a preguntar.
     */
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

    /**
     * Confirmar de nuevo el mismo nombre corrige a quién apunta, no explota
     * contra la unique ni deja dos filas contradiciéndose.
     */
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
