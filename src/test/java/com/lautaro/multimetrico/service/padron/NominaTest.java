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

@SpringBootTest
class NominaTest {

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
    void agregaAUnaPersonaQueFaltaba() {
        padrones.agregarAsesor(periodo, new Asesor("z400", "OJEDA SEBASTIAN", "LEDESMA JORGE"));

        assertThat(padrones.buscar(periodo).asesores())
                .extracting(Asesor::legajo)
                .containsExactlyInAnyOrder("z100", "z200", "z300", "z400");
    }

    @Test
    void laPersonaAgregadaApareceEnSuEquipo() {
        padrones.agregarAsesor(periodo, new Asesor("z400", "OJEDA SEBASTIAN", "PERALTA SILVIA"));

        assertThat(padrones.equipo(periodo, "PERALTA SILVIA"))
                .extracting(Asesor::legajo)
                .contains("z400");
    }

    @Test
    void unLegajoRepetidoSeRechaza() {
        assertThatThrownBy(() ->
                padrones.agregarAsesor(periodo, new Asesor("z100", "OTRA PERSONA", "LEDESMA JORGE")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("z100")
                .hasMessageContaining("AGUIRRE FLAVIA YAMILA");
    }


    @Test
    void corrigeElNombre() {
        padrones.corregirAsesor(periodo, "z100", "AGUIRRE FLAVIA Y.", "PERALTA SILVIA");

        assertThat(padrones.buscar(periodo).porLegajo("z100"))
                .get()
                .satisfies(asesor -> assertThat(asesor.nombre()).isEqualTo("AGUIRRE FLAVIA Y."));
    }

    @Test
    void mudarDeEquipoLaSacaDeUnoYLaPoneEnElOtro() {
        padrones.corregirAsesor(periodo, "z100", "AGUIRRE FLAVIA YAMILA", "LEDESMA JORGE");

        assertThat(padrones.equipo(periodo, "PERALTA SILVIA"))
                .extracting(Asesor::legajo)
                .containsExactly("z200");
        assertThat(padrones.equipo(periodo, "LEDESMA JORGE"))
                .extracting(Asesor::legajo)
                .containsExactlyInAnyOrder("z100", "z300");
    }

    @Test
    void corregirUnLegajoQueNoEstaFalla() {
        assertThatThrownBy(() -> padrones.corregirAsesor(periodo, "no-existe", "X", "Y"))
                .isInstanceOf(NoSuchElementException.class);
    }


    @Test
    void sacaAlAsesorDeLaNomina() {
        Asesor quitado = padrones.quitarAsesor(periodo, "z200");

        assertThat(quitado.legajo()).isEqualTo("z200");
        assertThat(padrones.buscar(periodo).asesores())
                .extracting(Asesor::legajo)
                .containsExactlyInAnyOrder("z100", "z300");
    }

    @Test
    void dejaDeAparecerEnSuEquipo() {
        assertThat(padrones.equipo(periodo, "PERALTA SILVIA")).hasSize(2);

        padrones.quitarAsesor(periodo, "z200");

        assertThat(padrones.equipo(periodo, "PERALTA SILVIA"))
                .extracting(Asesor::legajo)
                .containsExactly("z100");
    }

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

    @Test
    void losDemasQuedanIntactos() {
        padrones.quitarAsesor(periodo, "z200");

        assertThat(padrones.buscar(periodo).porLegajo("z100")).isPresent()
                .get()
                .satisfies(asesor -> assertThat(asesor.nombre()).isEqualTo("AGUIRRE FLAVIA YAMILA"));
    }
}
