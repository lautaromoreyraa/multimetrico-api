package com.lautaro.multimetrico.service.importacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.FormatoDeNumero;
import com.lautaro.multimetrico.domain.Importacion;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.importacion.Informe;
import com.lautaro.multimetrico.enumeration.EscalaDeNumero;
import com.lautaro.multimetrico.enumeration.EstadoDeImportacion;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;
import com.lautaro.multimetrico.fixture.PlanillaSintetica;
import com.lautaro.multimetrico.mapper.ImportacionMapper;
import com.lautaro.multimetrico.service.medicion.MedicionService;
import com.lautaro.multimetrico.service.padron.PadronService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * La importación de punta a punta, sobre planillas armadas en el momento.
 *
 * <p><b>Período propio.</b> {@code @SpringBootTest} comparte el contexto —y con
 * él la base— entre clases de test, así que las mediciones se acumulan y un
 * pliego cargado no se puede pisar. Cada clase usa su propio {@link YearMonth}
 * para no chocar con las demás.
 */
@SpringBootTest
class ImportacionServiceTest {

    /**
     * Un período distinto por cada test, no solo por cada clase.
     *
     * <p>La base vive en el contexto de Spring, que se comparte entre
     * <b>todos</b> los tests: dos métodos de esta misma clase que importen FCR
     * para el mismo asesor y el mismo período se ven las mediciones el uno al
     * otro. Costó un fallo descubrirlo. Con un mes propio cada uno, cada test
     * observa solo lo suyo.
     */
    private static final AtomicInteger MES = new AtomicInteger();

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

    @Autowired
    private ImportacionMapper mapper;

    @BeforeEach
    void cargarPadron() {
        periodo = YearMonth.of(2031, 1).plusMonths(MES.getAndIncrement());
        corte = periodo.atDay(20);
        padrones.cargar(new Padron(periodo, List.of(
                new Asesor(FLAVIA.legajo(), FLAVIA.nombre(), FLAVIA.lider()),
                new Asesor(HECTOR.legajo(), HECTOR.nombre(), HECTOR.lider()),
                new Asesor(ANA.legajo(), ANA.nombre(), ANA.lider()),
                new Asesor(ANABELA.legajo(), ANABELA.nombre(), ANABELA.lider()))));
    }

    private PerfilDeImportacion perfil(String nombre) {
        return servicio.definirPerfil(new PerfilDeImportacion(
                nombre, Metrica.FCR, "Datos", 1, "ASESOR", "VALOR", "LIDER",
                new FormatoDeNumero(SeparadorDecimal.COMA, EscalaDeNumero.TAL_CUAL)));
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
        Informe informe = mapper.informeDe(resultado);

        assertThat(resultado.estado()).isEqualTo(EstadoDeImportacion.TERMINADA);
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

        Informe informe = mapper.informeDe(importar(perfil("fcr-rechazo"), archivo));

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

        assertThat(resultado.estado()).isEqualTo(EstadoDeImportacion.RECHAZADA);
        assertThat(resultado.importadas()).isZero();
        assertThat(mapper.desajustesDe(resultado)).isNotEmpty();
        assertThat(mapper.desajustesDe(resultado).getFirst().toString()).contains("VALOR");
    }

    /**
     * Una importación rechazada <b>no cuadra</b>, aunque las cuentas den.
     *
     * <p>El invariante del {@link Informe} mira que las tres bolsas sumen las
     * filas leídas, y en una rechazada las cuatro cifras son cero: cero más cero
     * cierra. Así, el campo que existe para contestar "¿entró todo?" decía que sí
     * cuando no entró nada, que es exactamente la clase de mentira tranquila que
     * este módulo existe para no repetir.
     *
     * <p>La corrección vive en el mapper y no en el informe: el invariante sigue
     * siendo cierto y no se toca. Lo que cambia es la lectura que publica la API.
     */
    @Test
    void unaImportacionRechazadaNoCuadra() {
        byte[] archivo = PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "RESULTADO", "LIDER")
                .filaConValor("AGUIRRE FLAVIA Y", 80.0, "PERALTA SILVIA")
                .bytes();

        Importacion resultado = importar(perfil("fcr-rechazada-no-cuadra"), archivo);

        assertThat(resultado.estado()).isEqualTo(EstadoDeImportacion.RECHAZADA);
        assertThat(mapper.aResponse(resultado).cuadra()).isFalse();
        // Y el informe sigue cuadrando por dentro: son dos preguntas distintas.
        assertThat(mapper.informeDe(resultado).cuadra()).isTrue();
    }

    /** El nombre de la hoja también es parte del perfil. */
    @Test
    void unaHojaQueNoEstaEsDesajuste() {
        byte[] archivo = PlanillaSintetica.hoja("Otra").fila("ASESOR", "VALOR", "LIDER").bytes();

        Importacion resultado = importar(perfil("fcr-hoja"), archivo);

        assertThat(resultado.estado()).isEqualTo(EstadoDeImportacion.RECHAZADA);
        assertThat(mapper.desajustesDe(resultado).getFirst().toString()).contains("Otra");
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

        Informe informe = mapper.informeDe(importar(perfil("fcr-lider"), archivo));

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
        assertThat(mapper.informeDe(importada).pendientes()).hasSize(1);
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
            if (actual.estado() != EstadoDeImportacion.EN_CURSO) {
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
