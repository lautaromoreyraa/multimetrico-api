package com.lautaro.multimetrico.service.resolucion;

import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.domain.AliasDeNombre;
import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.dto.padron.Candidato;
import com.lautaro.multimetrico.dto.resolucion.Pendiente;
import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import com.lautaro.multimetrico.enumeration.Estrategia;
import com.lautaro.multimetrico.enumeration.MotivoDePendiente;
import com.lautaro.multimetrico.repository.AliasRepository;
import com.lautaro.multimetrico.util.NombreNormalizado;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Los nombres son inventados. Lo que se reproduce de la operación real es la
 * forma del problema: nombres cortados a un largo fijo, eñes rotas por la
 * codificación y dos personas con el mismo apellido en el mismo equipo.
 *
 * <p>El padrón se arma en memoria y nunca se guarda —el resolutor lo recibe
 * entero como argumento—, pero los alias sí van a la base: con
 * {@code AliasRepository} siendo una interfaz de Spring Data, el doble en memoria
 * que había antes tendría que implementar veinte métodos que nadie usa. Los
 * textos de este test llevan un prefijo propio para no cruzarse con los de otras
 * clases, porque la tabla de alias no tiene período y el contexto se comparte.
 */
@SpringBootTest
class ResolutorDeNombresTest {

    private static final Asesor FLAVIA = new Asesor("r100", "AGUIRRE FLAVIA YAMILA", "PERALTA SILVIA");
    private static final Asesor HECTOR = new Asesor("r200", "MIÑO HECTOR BAUTISTA", "PERALTA SILVIA");
    private static final Asesor ANA = new Asesor("r300", "SOSA ANA MARIA", "LEDESMA JORGE");
    private static final Asesor ANABELA = new Asesor("r400", "SOSA ANABELA ROSA", "LEDESMA JORGE");

    private final Padron padron = new Padron(YearMonth.of(2026, 8), List.of(FLAVIA, HECTOR, ANA, ANABELA));

    @Autowired
    private ResolutorDeNombres resolutor;

    @Autowired
    private AliasRepository alias;

    private static AsesorResponse comoResponse(Asesor asesor) {
        return new AsesorResponse(asesor.legajo(), asesor.nombre(), asesor.lider());
    }

    private static Candidato comoCandidato(Asesor asesor) {
        return new Candidato(asesor.legajo(), asesor.nombre());
    }

    @Test
    void resuelvePorLegajoCuandoElArchivoLoTrae() {
        Resolucion resolucion = resolutor.resolver(padron, List.of("r100"));

        assertThat(resolucion.completa()).isTrue();
        assertThat(resolucion.resueltos().getFirst().estrategia()).isEqualTo(Estrategia.LEGAJO);
    }

    @Test
    void resuelvePorNombreAunqueCambieMayusculasYAcentos() {
        Resolucion resolucion = resolutor.resolver(padron, List.of("mino hector bautista"));

        assertThat(resolucion.completa()).isTrue();
        assertThat(resolucion.resueltos().getFirst().asesor()).isEqualTo(comoResponse(HECTOR));
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
        assertThat(resolucion.resueltos().getFirst().asesor()).isEqualTo(comoResponse(FLAVIA));
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
        assertThat(pendiente.motivo()).isEqualTo(MotivoDePendiente.AMBIGUO);
        assertThat(pendiente.candidatos())
                .containsExactlyInAnyOrder(comoCandidato(ANA), comoCandidato(ANABELA));
    }

    /**
     * Una eñe rota por la codificación no empalma sola, pero el sistema no la
     * descarta en silencio: la deja pendiente con el candidato que corresponde.
     */
    @Test
    void unNombreConLaCodificacionRotaQuedaPendienteConSugerencia() {
        Resolucion resolucion = resolutor.resolver(padron, List.of("MI�O HECTOR BAUTISTA"));

        assertThat(resolucion.completa()).isFalse();
        Pendiente pendiente = resolucion.pendientes().getFirst();
        assertThat(pendiente.motivo()).isEqualTo(MotivoDePendiente.SIN_CANDIDATOS);
        assertThat(pendiente.candidatos()).contains(comoCandidato(HECTOR));
    }

    /**
     * Confirmado una vez, el mismo texto no vuelve a preguntar nunca.
     *
     * <p>Usa un texto <b>propio</b> y no el mismo de arriba a propósito: los alias
     * son conocimiento acumulado y su tabla no tiene período, así que lo que este
     * test confirma queda visible para todos los demás. Compartir el texto hacía
     * que, según el orden en que corrieran, el de la codificación rota encontrara
     * el alias que dejó éste y viera un nombre resuelto donde esperaba un
     * pendiente.
     */
    @Test
    void elAliasConfirmadoResuelveLaProximaVez() {
        String roto = "HECTOR MI�O B";
        assertThat(resolutor.resolver(padron, List.of(roto)).completa()).isFalse();

        alias.save(new AliasDeNombre(NombreNormalizado.de(roto).valor(), HECTOR.legajo()));

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
        List<String> nombres = List.of("r100", "mino hector bautista", "AGUIRRE FLAVIA Y", "SOSA ANA", "NADIE");

        Resolucion resolucion = resolutor.resolver(padron, nombres);

        assertThat(resolucion.resueltos().size() + resolucion.pendientes().size()).isEqualTo(nombres.size());
    }
}
