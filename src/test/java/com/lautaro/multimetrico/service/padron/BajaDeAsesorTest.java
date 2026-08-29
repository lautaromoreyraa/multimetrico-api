package com.lautaro.multimetrico.service.padron;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Padron;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sacar a una persona de la nómina del mes.
 *
 * <p>Existe porque corregir una sola fila no puede obligar a recargar el padrón
 * entero. Antes de esto, quitar a alguien significaba volver a subir las
 * cincuenta personas.
 *
 * <p><b>Período propio por test</b>: la base H2 se comparte entre clases y entre
 * métodos, así que dos tests que toquen el padrón del mismo mes se ven entre sí.
 */
@SpringBootTest
class BajaDeAsesorTest {

    private static final AtomicInteger MES = new AtomicInteger();

    @Autowired
    private PadronService padrones;

    private YearMonth periodo;

    @BeforeEach
    void cargarPadron() {
        periodo = YearMonth.of(2070, 1).plusMonths(MES.getAndIncrement());
        padrones.cargar(new Padron(periodo, List.of(
                new Asesor("z100", "AGUIRRE FLAVIA YAMILA", "PERALTA SILVIA"),
                new Asesor("z200", "MIÑO HECTOR BAUTISTA", "PERALTA SILVIA"),
                new Asesor("z300", "SOSA ANA MARIA", "LEDESMA JORGE"))));
    }

    @Test
    void sacaAlAsesorDeLaNomina() {
        Asesor quitado = padrones.quitarAsesor(periodo, "z200");

        assertThat(quitado.legajo()).isEqualTo("z200");
        assertThat(padrones.buscar(periodo).asesores())
                .extracting(Asesor::legajo)
                .containsExactlyInAnyOrder("z100", "z300");
    }

    /** Y con eso desaparece del equipo, que es el punto de la baja. */
    @Test
    void dejaDeAparecerEnSuEquipo() {
        assertThat(padrones.equipo(periodo, "PERALTA SILVIA")).hasSize(2);

        padrones.quitarAsesor(periodo, "z200");

        assertThat(padrones.equipo(periodo, "PERALTA SILVIA"))
                .extracting(Asesor::legajo)
                .containsExactly("z100");
    }

    /**
     * Quitar al último de un equipo deja al líder sin equipo, y eso se nota.
     *
     * <p>No es un error: es el resultado correcto de sacar a la única persona que
     * tenía. Lo que importa es que el líder desaparezca de la lista en vez de
     * quedar como un equipo vacío que la pantalla ofrece y no muestra nada.
     */
    @Test
    void unLiderSinNadieDejaDeEstarEnLaLista() {
        padrones.quitarAsesor(periodo, "z300");

        assertThat(padrones.lideres(periodo)).containsExactly("PERALTA SILVIA");
    }

    @Test
    void unLegajoQueNoEstaFallaDiciendoloYNoEnSilencio() {
        assertThatThrownBy(() -> padrones.quitarAsesor(periodo, "no-existe"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("no-existe");
    }

    /**
     * La baja no borra el resto del padrón.
     *
     * <p>Suena obvio pero es justo lo que rompería un {@code orphanRemoval} mal
     * usado: guardar el padrón con la lista reemplazada en vez de modificada
     * borraría todas las filas y volvería a insertarlas, perdiendo los ids.
     */
    @Test
    void losDemasQuedanIntactos() {
        padrones.quitarAsesor(periodo, "z200");

        assertThat(padrones.buscar(periodo).porLegajo("z100")).isPresent()
                .get()
                .satisfies(asesor -> assertThat(asesor.nombre()).isEqualTo("AGUIRRE FLAVIA YAMILA"));
    }
}
