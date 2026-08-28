package com.lautaro.multimetrico.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.dto.medicion.MedicionesVigentes;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.service.medicion.MedicionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Lo que la base tiene que seguir cumpliendo del contrato de
 * {@link MedicionRepository}.
 *
 * <p>Cada test usa su propio período, como el resto de la suite: la base se
 * comparte entre clases y las mediciones se acumulan porque nunca se pisan. Los
 * meses son <b>2045</b> y no 2040 a propósito: {@code MedicionServiceTest} ocupa
 * los 2040 y dos de sus tests miran la historia del mes <em>entero</em>, así que
 * una fila de acá con otro legajo igual los rompería.
 */
@SpringBootTest
class MedicionRepositoryTest {

    @Autowired
    private MedicionRepository repositorio;

    /**
     * El desempate dejó de ser un método de {@code MedicionesVigentes} y pasó a la
     * capa de servicio, así que los tests que lo verifican lo piden inyectado.
     */
    @Autowired
    private MedicionService mediciones;

    private static Medicion medicion(YearMonth periodo, Metrica metrica, String valor, int diaDeCorte) {
        return new Medicion("p100", metrica, periodo, new BigDecimal(valor), periodo.atDay(diaDeCorte), null);
    }

    /**
     * <b>La trampa de todo el módulo.</b>
     *
     * <p>Dos mediciones del mismo corte son el mismo archivo subido dos veces, y
     * la segunda es la corrección. Gana la última cargada, y quien decide eso es
     * el orden en que entraron —el id—, no la fecha de corte, que en las dos es
     * la misma.
     *
     * <p>Ojo con confiar solo en este test. Se probó cambiando el
     * {@code ORDER BY id} de la consulta por {@code ORDER BY fecha_de_corte}, y
     * <b>este siguió pasando</b>: con las dos fechas iguales el motor devolvió
     * las filas en el orden en que estaban, que casualmente es el de carga. Los
     * que sí se cayeron son los dos de acá abajo, donde los cortes son distintos
     * y desordenados. Los tres van juntos: este dice qué se espera, y esos dos
     * son los que lo defienden.
     *
     * <p>En producción el síntoma sería que el multimétrico muestra el número
     * viejo, sin ningún error ni log.
     */
    @Test
    void conElMismoCorteGanaLaUltimaCargadaTambienCuandoVieneDeLaBase() {
        YearMonth periodo = YearMonth.of(2045, 1);

        repositorio.saveAll(List.of(medicion(periodo, Metrica.TMO, "690", 22)));
        repositorio.saveAll(List.of(medicion(periodo, Metrica.TMO, "612", 22)));

        MedicionesVigentes vigentes = mediciones.vigentesDe(periodo, "p100");

        assertThat(vigentes.de(Metrica.TMO)).get()
                .satisfies(vigente -> assertThat(vigente.valor()).isEqualByComparingTo("612"));
    }

    /** La historia es el orden de carga, no la cronología de los cortes. */
    @Test
    void laHistoriaSaleEnOrdenDeCargaAunqueLosCortesLleguenAlReves() {
        YearMonth periodo = YearMonth.of(2045, 2);

        repositorio.saveAll(List.of(medicion(periodo, Metrica.FCR, "78.2", 22)));
        repositorio.saveAll(List.of(medicion(periodo, Metrica.FCR, "74.1", 15)));

        assertThat(repositorio.findByPeriodoAndLegajoClaveOrderByIdAsc(periodo, Medicion.clave("p100")))
                .extracting(medicion -> medicion.valor().toPlainString())
                .containsExactly("78.2", "74.1");

        // Y una carga tardía con corte viejo sigue sin desplazar a la vigente.
        assertThat(mediciones.vigentesDe(periodo, "p100").de(Metrica.FCR)).get()
                .satisfies(vigente -> assertThat(vigente.valor()).isEqualByComparingTo("78.2"));
    }

    /** La historia de todo el mes también, no solo la de un asesor. */
    @Test
    void laHistoriaDelMesEnteroTambienRespetaElOrdenDeCarga() {
        YearMonth periodo = YearMonth.of(2045, 3);

        repositorio.saveAll(List.of(
                new Medicion("p900", Metrica.NPS, periodo, new BigDecimal("55.4"), periodo.atDay(22), null)));
        repositorio.saveAll(List.of(
                new Medicion("p901", Metrica.NPS, periodo, new BigDecimal("61.0"), periodo.atDay(8), null)));

        assertThat(repositorio.findByPeriodoOrderByIdAsc(periodo))
                .extracting(Medicion::legajo)
                .containsExactly("p900", "p901");
    }

    /**
     * El descuido que no se ve: un {@code DECIMAL} sin escala en MySQL es
     * {@code DECIMAL(10,0)} y guarda 80 donde entró 80,04. No falla nada —el
     * valor sigue siendo un número válido—, pero cae en otra banda y el alcance
     * da distinto. El 80,04 es justo el caso del PLAN: el FCR que en la planilla
     * real no caía en ningún tramo.
     */
    @Test
    void unValorConDecimalesNoSePierdeAlGuardarlo() {
        YearMonth periodo = YearMonth.of(2045, 4);

        repositorio.saveAll(List.of(medicion(periodo, Metrica.FCR, "80.04", 22)));

        assertThat(repositorio.findByPeriodoAndLegajoClaveOrderByIdAsc(periodo, Medicion.clave("p100")))
                .singleElement()
                .satisfies(medicion -> assertThat(medicion.valor()).isEqualByComparingTo("80.04"));
    }

    /**
     * El valor vuelve con la escala con la que entró y no con los seis decimales
     * de la columna. No es cosmético: los mensajes que lee una persona se arman
     * con {@code toPlainString()}, y "sobran 12.000000" es otra cosa que "sobran
     * 12".
     *
     * <p>Quien lo garantiza cambió de lugar: antes era el adaptador al traducir
     * entidad a dominio, ahora es el accesor {@code Medicion.valor()}, que aplica
     * {@code DecimalUtil.comoEntro}. La garantía es la misma y por eso el test no
     * cambia.
     */
    @Test
    void elValorVuelveConLaEscalaConLaQueEntroYNoConLaDeLaColumna() {
        YearMonth periodo = YearMonth.of(2045, 5);

        repositorio.saveAll(List.of(
                medicion(periodo, Metrica.TMO, "612", 22),
                medicion(periodo, Metrica.FCR, "78.2", 22)));

        assertThat(repositorio.findByPeriodoAndLegajoClaveOrderByIdAsc(periodo, Medicion.clave("p100")))
                .extracting(medicion -> medicion.valor().toPlainString())
                .containsExactly("612", "78.2");
    }

    /** Los archivos de origen escriben el mismo legajo de las dos formas. */
    @Test
    void elLegajoSeBuscaSinDistinguirMayusculas() {
        YearMonth periodo = YearMonth.of(2045, 6);

        repositorio.saveAll(List.of(
                new Medicion("p777", Metrica.FCR, periodo, new BigDecimal("70"), periodo.atDay(8), null)));

        assertThat(repositorio.findByPeriodoAndLegajoClaveOrderByIdAsc(periodo, Medicion.clave("P777")))
                .hasSize(1);
    }

    /** La medición se guarda con el id de la importación que la trajo, o sin ninguno. */
    @Test
    void laMedicionGuardaDeQueImportacionSalioYTambienQueNoSalioDeNinguna() {
        YearMonth periodo = YearMonth.of(2045, 7);
        LocalDate corte = periodo.atDay(15);

        repositorio.saveAll(List.of(
                new Medicion("p100", Metrica.FCR, periodo, new BigDecimal("70"), corte, "imp-1"),
                new Medicion("p100", Metrica.TMO, periodo, new BigDecimal("640"), corte, null)));

        assertThat(repositorio.findByPeriodoAndLegajoClaveOrderByIdAsc(periodo, Medicion.clave("p100")))
                .extracting(Medicion::origenImportacion)
                .containsExactly("imp-1", null);
    }

    /**
     * El id lo asigna la base y es lo que ordena la historia.
     *
     * <p>No estaba en la versión anterior: con el modelo único la entidad que
     * devuelve {@code saveAll} <b>es</b> la que después se lee, así que vale
     * dejar escrito que sale con id. Si algún día alguien cambiara la estrategia
     * de generación por una asignada a mano, el desempate por orden de carga
     * dejaría de significar lo que significa.
     */
    @Test
    void laMedicionGuardadaVuelveConSuIdAsignado() {
        YearMonth periodo = YearMonth.of(2045, 8);

        List<Medicion> guardadas = repositorio.saveAll(List.of(medicion(periodo, Metrica.NPS, "55.4", 22)));

        assertThat(guardadas).singleElement()
                .satisfies(medicion -> assertThat(medicion.id()).isNotNull());
    }
}
