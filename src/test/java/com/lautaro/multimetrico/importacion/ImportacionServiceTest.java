package com.lautaro.multimetrico.importacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.importacion.domain.FormatoDeNumero;
import com.lautaro.multimetrico.importacion.domain.Importacion;
import com.lautaro.multimetrico.importacion.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.medicion.MedicionService;
import com.lautaro.multimetrico.padron.PadronService;
import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.padron.domain.Padron;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * La importación de punta a punta, sobre planillas armadas en el momento.
 *
 * <p><b>Período propio.</b> {@code @SpringBootTest} comparte el contexto —y con
 * él los repositorios en memoria— entre clases de test, así que las mediciones se
 * acumulan y un pliego cargado no se puede pisar. Cada clase usa su propio
 * {@link YearMonth} para no chocar con las demás.
 */
@SpringBootTest
class ImportacionServiceTest {

    /**
     * Un período distinto por cada test, no solo por cada clase.
     *
     * <p>Los repositorios en memoria viven en el contexto de Spring, que se
     * comparte entre <b>todos</b> los tests: dos métodos de esta misma clase que
     * importen FCR para el mismo asesor y el mismo período se ven las mediciones
     * el uno al otro. Costó un fallo descubrirlo. Con un mes propio cada uno, cada
     * test observa solo lo suyo.
     */
    private static final java.util.concurrent.atomic.AtomicInteger MES = new java.util.concurrent.atomic.AtomicInteger();

    private YearMonth periodo;
    private LocalDate corte;

    private static final Asesor FLAVIA = new Asesor("i100", "AGUIRRE FLAVIA YAMILA", "PERALTA SILVIA");
    private static final Asesor HECTOR = new Asesor("i200", "MIÑO HECTOR BAUTISTA", "PERALTA SILVIA");
    private static final Asesor ANA = new Asesor("i300", "SOSA ANA MARIA", "LEDESMA JORGE");
    private static final Asesor ANABELA = new Asesor("i400", "SOSA ANABELA ROSA", "LEDESMA JORGE");

    @Autowired
    private ImportacionService servicio;

    @Autowired
    private PadronService padrones;

    @Autowired
    private MedicionService mediciones;

    @BeforeEach
    void cargarPadron() {
        periodo = YearMonth.of(2031, 1).plusMonths(MES.getAndIncrement());
        corte = periodo.atDay(20);
        padrones.cargar(new Padron(periodo, List.of(FLAVIA, HECTOR, ANA, ANABELA)));
    }

    private PerfilDeImportacion perfil(String nombre) {
        return servicio.definirPerfil(new PerfilDeImportacion(
                nombre, Metrica.FCR, "Datos", 1, "ASESOR", "VALOR", "LIDER",
                new FormatoDeNumero(FormatoDeNumero.SeparadorDecimal.COMA,
                        FormatoDeNumero.Escala.TAL_CUAL)));
    }

    /**
     * La propiedad que define el módulo: cada fila del archivo termina en
     * exactamente uno de los tres montones, y los tres suman lo que se leyó.
     *
     * <p>Es lo que la planilla no garantiza. Ahí una fila que no empalma no
     * produce ningún síntoma: el total sale igual, un poco más chico.
     */
    @Test
    void ningunaFilaSePierde() {
        byte[] archivo = PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "VALOR", "LIDER")
                .filaConValor("AGUIRRE FLAVIA Y", 82.4, "PERALTA SILVIA")   // prefijo único
                .filaConValor("MINO HECTOR BAUTISTA", 79.1, "PERALTA SILVIA") // nombre exacto
                .filaConValor("SOSA ANA", 75.0, "LEDESMA JORGE")            // ambiguo -> pendiente
                .filaConValor("PEREZ QUIENSABE", 60.0, "LEDESMA JORGE")     // nadie -> pendiente
                .fila("QUEDO SIN VALOR", "", "PERALTA SILVIA")              // sin valor -> rechazo
                .fila("", "70", "PERALTA SILVIA")                           // sin asesor -> rechazo
                .filaVacia()                                                 // no se cuenta
                .bytes();

        Importacion resultado = importar(perfil("fcr-completo"), archivo);
        var informe = resultado.informe();

        assertThat(resultado.estado()).isEqualTo(Importacion.Estado.TERMINADA);
        assertThat(informe.filasLeidas()).isEqualTo(6);
        assertThat(informe.importadas()).isEqualTo(2);
        assertThat(informe.rechazadas()).hasSize(2);
        assertThat(informe.pendientes()).hasSize(2);
        assertThat(informe.cuadra()).isTrue();
    }

    /** El número de fila del rechazo es el que se ve en Excel, para poder ir a buscarlo. */
    @Test
    void elRechazoDiceQueFilaYPorQue() {
        byte[] archivo = PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "VALOR", "LIDER")
                .filaConValor("AGUIRRE FLAVIA Y", 80.0, "PERALTA SILVIA")
                .fila("MIÑO HECTOR BAUTISTA", "sin dato", "PERALTA SILVIA")
                .bytes();

        var informe = importar(perfil("fcr-rechazo"), archivo).informe();

        assertThat(informe.rechazadas()).singleElement().satisfies(rechazo -> {
            assertThat(rechazo.numeroDeFila()).isEqualTo(3);
            assertThat(rechazo.textoValor()).isEqualTo("sin dato");
        });
    }

    /**
     * Una celda con formato de porcentaje guarda 0,805 y muestra 80,5%. Si el
     * lector no lo detecta, el archivo entra cien veces más chico y nadie lo nota
     * hasta que todos los alcances dan mal.
     */
    @Test
    void unaCeldaConFormatoDePorcentajeSeInterpretaEnLasUnidadesDelPliego() {
        byte[] archivo = PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "VALOR", "LIDER")
                .filaConPorcentaje("AGUIRRE FLAVIA Y", 0.805, "PERALTA SILVIA")
                .bytes();

        importar(perfil("fcr-porcentaje"), archivo);

        assertThat(mediciones.historia(periodo, FLAVIA.legajo(), Metrica.FCR))
                .singleElement()
                .satisfies(m -> assertThat(m.valor()).isEqualByComparingTo("80.5"));
    }

    /**
     * Si el archivo no coincide con el perfil se rechaza entero. Es preferible a
     * importar bien la mitad: un archivo que no entró se nota, uno que entró mal
     * no.
     */
    @Test
    void unArchivoQueNoCoincideConElPerfilSeRechazaEntero() {
        byte[] archivo = PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "RESULTADO", "LIDER")
                .filaConValor("AGUIRRE FLAVIA Y", 80.0, "PERALTA SILVIA")
                .bytes();

        Importacion resultado = importar(perfil("fcr-desajustado"), archivo);

        assertThat(resultado.estado()).isEqualTo(Importacion.Estado.RECHAZADA);
        assertThat(resultado.informe().importadas()).isZero();
        assertThat(resultado.desajustes()).isNotEmpty();
        assertThat(resultado.desajustes().getFirst().toString()).contains("VALOR");
    }

    /** El nombre de la hoja también es parte del perfil. */
    @Test
    void unaHojaQueNoEstaEsDesajuste() {
        byte[] archivo = PlanillaSintetica.hoja("Otra").fila("ASESOR", "VALOR", "LIDER").bytes();

        Importacion resultado = importar(perfil("fcr-hoja"), archivo);

        assertThat(resultado.estado()).isEqualTo(Importacion.Estado.RECHAZADA);
        assertThat(resultado.desajustes().getFirst().toString()).contains("Otra");
    }

    /**
     * El padrón manda sobre el archivo, pero la diferencia se informa: casi
     * siempre significa que alguien cambió de equipo y el padrón quedó viejo.
     */
    @Test
    void siElArchivoDiceOtroLiderSeImportaIgualYSeAnotaLaDiferencia() {
        byte[] archivo = PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "VALOR", "LIDER")
                .filaConValor("AGUIRRE FLAVIA Y", 81.0, "LEDESMA JORGE")
                .bytes();

        var informe = importar(perfil("fcr-lider"), archivo).informe();

        assertThat(informe.importadas()).isEqualTo(1);
        assertThat(informe.discrepanciasDeLider()).singleElement().satisfies(d -> {
            assertThat(d.legajo()).isEqualTo(FLAVIA.legajo());
            assertThat(d.liderDelArchivo()).isEqualTo("LEDESMA JORGE");
            assertThat(d.liderDelPadron()).isEqualTo("PERALTA SILVIA");
        });
    }

    /**
     * Un pendiente se resuelve para esta fila de este archivo, no como alias
     * global: {@code SOSA ANA} apunta a dos personas y podría ser la otra el mes
     * que viene.
     */
    @Test
    void unPendienteSeResuelveAManoYProduceLaMedicion() {
        byte[] archivo = PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "VALOR", "LIDER")
                .filaConValor("SOSA ANA", 77.7, "LEDESMA JORGE")
                .bytes();

        Importacion importada = importar(perfil("fcr-pendiente"), archivo);
        assertThat(importada.informe().pendientes()).hasSize(1);
        assertThat(mediciones.historia(periodo, ANA.legajo(), Metrica.FCR)).isEmpty();

        servicio.resolverPendiente(importada.id(), 2, ANA.legajo());

        assertThat(mediciones.historia(periodo, ANA.legajo(), Metrica.FCR))
                .singleElement()
                .satisfies(m -> assertThat(m.valor()).isEqualByComparingTo("77.7"));
        assertThat(mediciones.historia(periodo, ANABELA.legajo(), Metrica.FCR)).isEmpty();
    }

    /** La medición queda atada a la importación que la trajo, para poder auditarla. */
    @Test
    void laMedicionRecuerdaDeQueImportacionSalio() {
        byte[] archivo = PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "VALOR", "LIDER")
                .filaConValor("MINO HECTOR BAUTISTA", 83.3, "PERALTA SILVIA")
                .bytes();

        Importacion resultado = importar(perfil("fcr-origen"), archivo);

        assertThat(mediciones.historia(periodo, HECTOR.legajo(), Metrica.FCR))
                .singleElement()
                .satisfies(m -> assertThat(m.origenImportacion()).isEqualTo(resultado.id()));
    }

    /**
     * Espera a que la importación deje de estar en curso.
     *
     * <p>El servicio procesa fuera del hilo de la petición, así que el test tiene
     * que esperar el resultado en vez de asumirlo.
     */
    private Importacion importar(PerfilDeImportacion perfil, byte[] archivo) {
        String id = servicio.recibir(perfil.nombre(), periodo, corte, archivo).id();

        for (int intento = 0; intento < 100; intento++) {
            Importacion actual = servicio.buscar(id);
            if (actual.estado() != Importacion.Estado.EN_CURSO) {
                return actual;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrumpido esperando la importación", e);
            }
        }
        throw new AssertionError("La importación %s no terminó a tiempo".formatted(id));
    }
}
