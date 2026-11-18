package com.lautaro.multimetrico.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.domain.FormatoDeNumero;
import com.lautaro.multimetrico.domain.Importacion;
import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.importacion.Desajuste;
import com.lautaro.multimetrico.dto.importacion.DiscrepanciaDeLider;
import com.lautaro.multimetrico.dto.importacion.FilaPendiente;
import com.lautaro.multimetrico.dto.importacion.FilaRechazada;
import com.lautaro.multimetrico.dto.importacion.Informe;
import com.lautaro.multimetrico.dto.padron.Candidato;
import com.lautaro.multimetrico.enumeration.EscalaDeNumero;
import com.lautaro.multimetrico.enumeration.EstadoDeImportacion;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.MotivoDePendiente;
import com.lautaro.multimetrico.enumeration.MotivoDeRechazo;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;
import com.lautaro.multimetrico.mapper.ImportacionMapper;
import com.lautaro.multimetrico.service.importacion.ImportacionService;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ImportacionRepositoryTest {

    @Autowired
    private ImportacionRepository importaciones;

    @Autowired
    private ImportacionMapper mapper;

    @Autowired
    private ImportacionService servicio;

    private static PerfilDeImportacion perfil(String nombre) {
        return new PerfilDeImportacion(nombre, Metrica.FCR, "Datos", 1, "ASESOR", "VALOR", "LIDER",
                new FormatoDeNumero(SeparadorDecimal.COMA, EscalaDeNumero.FRACCION_DE_UNO));
    }

    @Test
    void elInformeVuelveEnteroConSusTresMontones() {
        String id = "persistencia-informe-1";
        YearMonth periodo = YearMonth.of(2044, 1);

        Importacion enCurso = mapper.aEntidad(id, perfil("informe-completo"), periodo, periodo.atDay(20));

        mapper.terminar(enCurso, new Informe(
                3,
                1,
                List.of(new FilaRechazada(5, "QUEDO SIN VALOR", "", MotivoDeRechazo.VALOR_VACIO)),
                List.of(new FilaPendiente(4, "SOSA ANA", new BigDecimal("77.7"),
                        MotivoDePendiente.AMBIGUO,
                        List.of(new Candidato("i300", "SOSA ANA MARIA"),
                                new Candidato("i400", "SOSA ANABELA ROSA")))),
                List.of(new DiscrepanciaDeLider(2, "AGUIRRE FLAVIA Y", "i100",
                        "LEDESMA JORGE", "PERALTA SILVIA"))));
        importaciones.save(enCurso);

        Informe leido = mapper.informeDe(importaciones.findById(id).orElseThrow());

        assertThat(leido.filasLeidas()).isEqualTo(3);
        assertThat(leido.importadas()).isEqualTo(1);
        assertThat(leido.cuadra()).isTrue();
        assertThat(leido.rechazadas()).singleElement().satisfies(rechazo -> {
            assertThat(rechazo.numeroDeFila()).isEqualTo(5);
            assertThat(rechazo.motivo()).isEqualTo(MotivoDeRechazo.VALOR_VACIO);
        });
        assertThat(leido.pendientes()).singleElement().satisfies(pendiente -> {
            assertThat(pendiente.valor()).isEqualByComparingTo("77.7");
            assertThat(pendiente.candidatos()).extracting(Candidato::legajo)
                    .containsExactly("i300", "i400");
        });
        assertThat(leido.discrepanciasDeLider()).singleElement()
                .satisfies(discrepancia -> assertThat(discrepancia.liderDelPadron()).isEqualTo("PERALTA SILVIA"));
    }

    @Test
    void guardarDosVecesLaMismaImportacionActualizaLaFilaEnVezDeDuplicarla() {
        String id = "persistencia-informe-2";
        YearMonth periodo = YearMonth.of(2044, 2);

        Importacion enCurso = mapper.aEntidad(id, perfil("informe-estado"), periodo, periodo.atDay(20));
        importaciones.save(enCurso);

        assertThat(importaciones.findById(id).orElseThrow().estado())
                .isEqualTo(EstadoDeImportacion.EN_CURSO);

        Importacion leidaEnCurso = importaciones.findById(id).orElseThrow();
        mapper.rechazar(leidaEnCurso, List.of(Desajuste.columna("valor", "VALOR",
                List.of("ASESOR", "RESULTADO", "LIDER"))));
        importaciones.save(leidaEnCurso);

        Importacion leida = importaciones.findById(id).orElseThrow();

        assertThat(leida.estado()).isEqualTo(EstadoDeImportacion.RECHAZADA);
        assertThat(leida.terminada()).isNotNull();
        assertThat(mapper.desajustesDe(leida)).singleElement()
                .satisfies(desajuste -> assertThat(desajuste.toString()).contains("VALOR"));
        assertThat(importaciones.findAllByOrderByIniciadaDesc())
                .filteredOn(i -> i.id().equals(id)).hasSize(1);
    }

    @Test
    void elInstanteDeAltaLoPoneLaAuditoriaAlPersistir() {
        String id = "persistencia-informe-3";
        YearMonth periodo = YearMonth.of(2044, 3);

        importaciones.save(mapper.aEntidad(id, perfil("informe-auditoria"), periodo, periodo.atDay(20)));

        assertThat(importaciones.findById(id).orElseThrow().iniciada()).isNotNull();
    }

    @Test
    void guardarUnPerfilConElMismoNombreLoEditaEnVezDeDuplicarlo() {
        servicio.definirPerfil(perfil("fcr-semanal"));
        servicio.definirPerfil(new PerfilDeImportacion("FCR-Semanal", Metrica.FCR, "Datos", 1,
                "ASESOR", "RESULTADO", "LIDER", FormatoDeNumero.estandar()));

        assertThat(servicio.perfil("fcr-semanal").columnaValor()).isEqualTo("RESULTADO");
        assertThat(servicio.perfiles())
                .filteredOn(p -> p.nombre().equalsIgnoreCase("fcr-semanal")).hasSize(1);
    }

    @Test
    void elFormatoDeNumeroDelPerfilVuelveDeLaBase() {
        servicio.definirPerfil(perfil("fcr-fraccion"));

        assertThat(servicio.perfil("fcr-fraccion")).satisfies(guardado -> {
            assertThat(guardado.formatoNumero().escala()).isEqualTo(EscalaDeNumero.FRACCION_DE_UNO);
            assertThat(guardado.formatoNumero().separadorDecimal()).isEqualTo(SeparadorDecimal.COMA);
            assertThat(guardado.traeLider()).isTrue();
        });
    }

    @Test
    void unPerfilSinHojaVuelveSinHoja() {
        servicio.definirPerfil(new PerfilDeImportacion("sin-hoja", Metrica.NPS, null, 2,
                "ASESOR", "VALOR", null, FormatoDeNumero.estandar()));

        assertThat(servicio.perfil("sin-hoja")).satisfies(guardado -> {
            assertThat(guardado.cualquierHoja()).isTrue();
            assertThat(guardado.traeLider()).isFalse();
        });
    }
}
