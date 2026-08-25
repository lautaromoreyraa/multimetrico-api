package com.lautaro.multimetrico.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.importacion.ImportacionRepository;
import com.lautaro.multimetrico.importacion.PerfilRepository;
import com.lautaro.multimetrico.importacion.domain.Desajuste;
import com.lautaro.multimetrico.importacion.domain.DiscrepanciaDeLider;
import com.lautaro.multimetrico.importacion.domain.FilaPendiente;
import com.lautaro.multimetrico.importacion.domain.FilaRechazada;
import com.lautaro.multimetrico.importacion.domain.FormatoDeNumero;
import com.lautaro.multimetrico.importacion.domain.Importacion;
import com.lautaro.multimetrico.importacion.domain.Informe;
import com.lautaro.multimetrico.importacion.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ImportacionEnBaseTest {

    @Autowired
    private ImportacionRepository importaciones;

    @Autowired
    private PerfilRepository perfiles;

    private static PerfilDeImportacion perfil(String nombre) {
        return new PerfilDeImportacion(nombre, Metrica.FCR, "Datos", 1, "ASESOR", "VALOR", "LIDER",
                new FormatoDeNumero(FormatoDeNumero.SeparadorDecimal.COMA,
                        FormatoDeNumero.Escala.FRACCION_DE_UNO));
    }

    /**
     * El informe entero vuelve de las columnas de texto: los tres montones con su
     * contenido, y los contadores que tienen que cuadrar con ellos.
     *
     * <p>Que cuadre no es un detalle: {@link Informe} revienta al construirse si
     * los contadores y las listas no coinciden, así que este test también prueba
     * que los dos lados —columnas y JSON— se guardaron juntos.
     */
    @Test
    void elInformeVuelveEnteroConSusTresMontones() {
        String id = "persistencia-informe-1";
        Importacion enCurso = Importacion.enCurso(id, perfil("informe-completo"),
                YearMonth.of(2044, 1), YearMonth.of(2044, 1).atDay(20));

        // 3 filas leidas = 1 importada + 1 rechazada + 1 pendiente. Las
        // discrepancias de lider no suman: esa fila se importo igual.
        importaciones.guardar(enCurso.terminada(new Informe(
                3,
                1,
                List.of(new FilaRechazada(5, "QUEDO SIN VALOR", "", FilaRechazada.Motivo.VALOR_VACIO)),
                List.of(new FilaPendiente(4, "SOSA ANA", new BigDecimal("77.7"),
                        FilaPendiente.Motivo.AMBIGUO,
                        List.of(new FilaPendiente.Candidato("i300", "SOSA ANA MARIA"),
                                new FilaPendiente.Candidato("i400", "SOSA ANABELA ROSA")))),
                List.of(new DiscrepanciaDeLider(2, "AGUIRRE FLAVIA Y", "i100",
                        "LEDESMA JORGE", "PERALTA SILVIA")))));

        Informe leido = importaciones.porId(id).orElseThrow().informe();

        assertThat(leido.filasLeidas()).isEqualTo(3);
        assertThat(leido.importadas()).isEqualTo(1);
        assertThat(leido.cuadra()).isTrue();
        assertThat(leido.rechazadas()).singleElement().satisfies(rechazo -> {
            assertThat(rechazo.numeroDeFila()).isEqualTo(5);
            assertThat(rechazo.motivo()).isEqualTo(FilaRechazada.Motivo.VALOR_VACIO);
        });
        assertThat(leido.pendientes()).singleElement().satisfies(pendiente -> {
            assertThat(pendiente.valor()).isEqualByComparingTo("77.7");
            assertThat(pendiente.candidatos()).extracting(FilaPendiente.Candidato::legajo)
                    .containsExactly("i300", "i400");
        });
        assertThat(leido.discrepanciasDeLider()).singleElement()
                .satisfies(discrepancia -> assertThat(discrepancia.liderDelPadron()).isEqualTo("PERALTA SILVIA"));
    }

    /**
     * Una importación se guarda varias veces sobre la misma fila —en curso,
     * después terminada o rechazada, después cada pendiente que alguien resuelve
     * a mano—, así que guardar dos veces el mismo id actualiza en vez de duplicar.
     */
    @Test
    void guardarDosVecesLaMismaImportacionActualizaLaFilaEnVezDeDuplicarla() {
        String id = "persistencia-informe-2";
        Importacion enCurso = Importacion.enCurso(id, perfil("informe-estado"),
                YearMonth.of(2044, 2), YearMonth.of(2044, 2).atDay(20));

        importaciones.guardar(enCurso);
        assertThat(importaciones.porId(id).orElseThrow().estado()).isEqualTo(Importacion.Estado.EN_CURSO);

        importaciones.guardar(enCurso.rechazada(List.of(Desajuste.columna("valor", "VALOR",
                List.of("ASESOR", "RESULTADO", "LIDER")))));

        Importacion leida = importaciones.porId(id).orElseThrow();

        assertThat(leida.estado()).isEqualTo(Importacion.Estado.RECHAZADA);
        assertThat(leida.terminada()).isNotNull();
        assertThat(leida.desajustes()).singleElement()
                .satisfies(desajuste -> assertThat(desajuste.toString()).contains("VALOR"));
        assertThat(importaciones.todas()).filteredOn(i -> i.id().equals(id)).hasSize(1);
    }

    /**
     * El perfil es dato: guardarlo con un nombre que ya existe lo edita. Es lo que
     * pasa cada vez que la empresa renombra una columna del reporte.
     */
    @Test
    void guardarUnPerfilConElMismoNombreLoEditaEnVezDeDuplicarlo() {
        perfiles.guardar(perfil("fcr-semanal"));
        perfiles.guardar(new PerfilDeImportacion("FCR-Semanal", Metrica.FCR, "Datos", 1,
                "ASESOR", "RESULTADO", "LIDER", FormatoDeNumero.estandar()));

        assertThat(perfiles.porNombre("fcr-semanal")).get()
                .satisfies(perfil -> assertThat(perfil.columnaValor()).isEqualTo("RESULTADO"));
        assertThat(perfiles.todos()).filteredOn(p -> p.nombre().equalsIgnoreCase("fcr-semanal")).hasSize(1);
    }

    /** El formato de número del perfil vuelve entero, que es de lo que depende leer bien el archivo. */
    @Test
    void elFormatoDeNumeroDelPerfilVuelveDeLaBase() {
        perfiles.guardar(perfil("fcr-fraccion"));

        assertThat(perfiles.porNombre("fcr-fraccion")).get().satisfies(guardado -> {
            assertThat(guardado.formatoNumero().escala()).isEqualTo(FormatoDeNumero.Escala.FRACCION_DE_UNO);
            assertThat(guardado.formatoNumero().separadorDecimal())
                    .isEqualTo(FormatoDeNumero.SeparadorDecimal.COMA);
            assertThat(guardado.traeLider()).isTrue();
        });
    }

    /** "Cualquier hoja" es un null en el dominio y tiene que seguir siéndolo. */
    @Test
    void unPerfilSinHojaVuelveSinHoja() {
        perfiles.guardar(new PerfilDeImportacion("sin-hoja", Metrica.NPS, null, 2,
                "ASESOR", "VALOR", null, FormatoDeNumero.estandar()));

        assertThat(perfiles.porNombre("sin-hoja")).get().satisfies(guardado -> {
            assertThat(guardado.cualquierHoja()).isTrue();
            assertThat(guardado.traeLider()).isFalse();
        });
    }
}
