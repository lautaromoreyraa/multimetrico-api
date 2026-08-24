package com.lautaro.multimetrico.padron;

import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.padron.domain.Estrategia;
import com.lautaro.multimetrico.padron.domain.NombreNormalizado;
import com.lautaro.multimetrico.padron.domain.Padron;
import com.lautaro.multimetrico.padron.domain.Pendiente;
import com.lautaro.multimetrico.padron.domain.Resolucion;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Los nombres son inventados. Lo que se reproduce de la operación real es la
 * forma del problema: nombres cortados a un largo fijo, eñes rotas por la
 * codificación y dos personas con el mismo apellido en el mismo equipo.
 */
class ResolutorDeNombresTest {

    private static final Asesor FLAVIA = new Asesor("u100", "AGUIRRE FLAVIA YAMILA", "PERALTA SILVIA");
    private static final Asesor HECTOR = new Asesor("u200", "MIÑO HECTOR BAUTISTA", "PERALTA SILVIA");
    private static final Asesor ANA = new Asesor("u300", "SOSA ANA MARIA", "LEDESMA JORGE");
    private static final Asesor ANABELA = new Asesor("u400", "SOSA ANABELA ROSA", "LEDESMA JORGE");

    private final Padron padron = new Padron(YearMonth.of(2026, 8), List.of(FLAVIA, HECTOR, ANA, ANABELA));

    private RegistroDeAlias alias;
    private ResolutorDeNombres resolutor;

    @BeforeEach
    void prepararResolutor() {
        alias = new AliasEnMemoria();
        resolutor = new ResolutorDeNombres(alias);
    }

    @Test
    void resuelvePorLegajoCuandoElArchivoLoTrae() {
        Resolucion resolucion = resolutor.resolver(padron, List.of("u100"));

        assertThat(resolucion.completa()).isTrue();
        assertThat(resolucion.resueltos().getFirst().estrategia()).isEqualTo(Estrategia.LEGAJO);
    }

    @Test
    void resuelvePorNombreAunqueCambieMayusculasYAcentos() {
        Resolucion resolucion = resolutor.resolver(padron, List.of("mino hector bautista"));

        assertThat(resolucion.completa()).isTrue();
        assertThat(resolucion.resueltos().getFirst().asesor()).isEqualTo(HECTOR);
        assertThat(resolucion.resueltos().getFirst().estrategia()).isEqualTo(Estrategia.NOMBRE_EXACTO);
    }

    /**
     * El caso más común: la planilla corta el nombre a un largo fijo, a mitad de
     * palabra. Así llega en los archivos reales.
     */
    @Test
    void resuelveUnNombreCortadoSiApuntaAUnaSolaPersona() {
        Resolucion resolucion = resolutor.resolver(padron, List.of("AGUIRRE FLAVIA Y"));

        assertThat(resolucion.completa()).isTrue();
        assertThat(resolucion.resueltos().getFirst().asesor()).isEqualTo(FLAVIA);
        assertThat(resolucion.resueltos().getFirst().estrategia()).isEqualTo(Estrategia.PREFIJO_UNICO);
    }

    /**
     * Dos personas del mismo apellido y el nombre cortado antes de poder
     * distinguirlas. El sistema no elige: eso lo desempata una persona.
     */
    @Test
    void noAdivinaCuandoElNombreCortadoDaParaDos() {
        Resolucion resolucion = resolutor.resolver(padron, List.of("SOSA ANA"));

        assertThat(resolucion.completa()).isFalse();
        Pendiente pendiente = resolucion.pendientes().getFirst();
        assertThat(pendiente.motivo()).isEqualTo(Pendiente.Motivo.AMBIGUO);
        assertThat(pendiente.candidatos()).containsExactlyInAnyOrder(ANA, ANABELA);
    }

    /**
     * Una eñe rota por la codificación no empalma sola, pero el sistema no la
     * descarta en silencio: la deja pendiente con el candidato que corresponde.
     */
    @Test
    void unNombreConLaCodificacionRotaQuedaPendienteConSugerencia() {
        Resolucion resolucion = resolutor.resolver(padron, List.of("MI\uFFFDO HECTOR BAUTISTA"));

        assertThat(resolucion.completa()).isFalse();
        Pendiente pendiente = resolucion.pendientes().getFirst();
        assertThat(pendiente.motivo()).isEqualTo(Pendiente.Motivo.SIN_CANDIDATOS);
        assertThat(pendiente.candidatos()).contains(HECTOR);
    }

    /** Confirmado una vez, el mismo texto no vuelve a preguntar nunca. */
    @Test
    void elAliasConfirmadoResuelveLaProximaVez() {
        String roto = "MI\uFFFDO HECTOR BAUTISTA";
        assertThat(resolutor.resolver(padron, List.of(roto)).completa()).isFalse();

        alias.confirmar(NombreNormalizado.de(roto), HECTOR.legajo());

        Resolucion despues = resolutor.resolver(padron, List.of(roto));
        assertThat(despues.completa()).isTrue();
        assertThat(despues.resueltos().getFirst().estrategia()).isEqualTo(Estrategia.ALIAS_CONFIRMADO);
    }

    @Test
    void unNombreQueNoEsDeNadieQuedaListado() {
        Resolucion resolucion = resolutor.resolver(padron, List.of("ZZZZ QUIENSABE"));

        assertThat(resolucion.pendientes()).hasSize(1);
        assertThat(resolucion.pendientes().getFirst().candidatos()).isEmpty();
    }

    /**
     * Lo que no empalma no se descarta: la suma de resueltos y pendientes es
     * siempre la cantidad de filas que entraron. Es la propiedad que la planilla
     * no tiene, y por la que hoy la gente desaparece del multimétrico.
     */
    @Test
    void nadaSePierdeEnElCamino() {
        List<String> nombres = List.of("u100", "mino hector bautista", "AGUIRRE FLAVIA Y", "SOSA ANA", "NADIE");

        Resolucion resolucion = resolutor.resolver(padron, nombres);

        assertThat(resolucion.resueltos().size() + resolucion.pendientes().size()).isEqualTo(nombres.size());
    }
}
