package com.lautaro.multimetrico.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.padron.PadronRepository;
import com.lautaro.multimetrico.padron.RegistroDeAlias;
import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.padron.domain.NombreNormalizado;
import com.lautaro.multimetrico.padron.domain.Padron;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PadronEnBaseTest {

    @Autowired
    private PadronRepository padrones;

    @Autowired
    private RegistroDeAlias alias;

    /**
     * El padrón <b>sí</b> se reemplaza: es una lista de gente que cambia dentro
     * del mes. Lo que no puede pasar es que las dos versiones queden mezcladas y
     * el equipo aparezca con el doble de asesores, o con alguien que ya se fue.
     */
    @Test
    void cargarElPadronDeUnPeriodoDosVecesLoReemplazaEnVezDeAcumularlo() {
        YearMonth periodo = YearMonth.of(2042, 1);

        padrones.guardar(new Padron(periodo, List.of(
                new Asesor("q100", "AGUIRRE FLAVIA YAMILA", "PERALTA SILVIA"),
                new Asesor("q200", "MIÑO HECTOR BAUTISTA", "PERALTA SILVIA"))));

        padrones.guardar(new Padron(periodo, List.of(
                new Asesor("q100", "AGUIRRE FLAVIA YAMILA", "LEDESMA JORGE"))));

        Padron guardado = padrones.buscar(periodo).orElseThrow();

        assertThat(guardado.tamano()).isEqualTo(1);
        assertThat(guardado.lideres()).containsExactly("LEDESMA JORGE");
    }

    /** Los apellidos con eñe y con acentos tienen que volver enteros: la base es utf8mb4. */
    @Test
    void losNombresConEnieYAcentosVuelvenIgualDeLaBase() {
        YearMonth periodo = YearMonth.of(2042, 2);

        padrones.guardar(new Padron(periodo, List.of(
                new Asesor("q300", "MIÑO HÉCTOR BAUTISTA", "PERALTA SILVIA"))));

        assertThat(padrones.buscar(periodo).orElseThrow().porLegajo("q300")).get()
                .satisfies(asesor -> assertThat(asesor.nombre()).isEqualTo("MIÑO HÉCTOR BAUTISTA"));
    }

    @Test
    void losPeriodosSalenDelMasNuevoAlMasViejo() {
        padrones.guardar(new Padron(YearMonth.of(2043, 3),
                List.of(new Asesor("q400", "UNA PERSONA", "UN LIDER"))));
        padrones.guardar(new Padron(YearMonth.of(2043, 11),
                List.of(new Asesor("q400", "UNA PERSONA", "UN LIDER"))));

        assertThat(padrones.periodos())
                .containsSubsequence(YearMonth.of(2043, 11), YearMonth.of(2043, 3));
    }

    /**
     * Los alias son conocimiento que se acumula entre meses y no cuelgan de
     * ningún período: si en marzo alguien confirmó qué legajo es cierto texto mal
     * codificado, en abril el sistema no vuelve a preguntar.
     */
    @Test
    void unAliasConfirmadoSobreviveYNoDependeDelPeriodo() {
        NombreNormalizado nombre = NombreNormalizado.de("AGUIRRE FLAVIA Y");

        alias.confirmar(nombre, "q500");

        assertThat(alias.legajoDe(nombre)).contains("q500");
        assertThat(alias.aliasDe("q500")).contains("AGUIRRE FLAVIA Y");
    }

    /**
     * Confirmar de nuevo el mismo nombre corrige a quién apunta, no explota
     * contra la unique ni deja dos filas contradiciéndose.
     */
    @Test
    void volverAConfirmarUnAliasLoReapunta() {
        NombreNormalizado nombre = NombreNormalizado.de("SOSA ANA");

        alias.confirmar(nombre, "q600");
        alias.confirmar(nombre, "q700");

        assertThat(alias.legajoDe(nombre)).contains("q700");
        assertThat(alias.aliasDe("q600")).doesNotContain("SOSA ANA");
    }
}
