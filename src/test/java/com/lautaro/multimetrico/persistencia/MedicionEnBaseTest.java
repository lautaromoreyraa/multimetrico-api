package com.lautaro.multimetrico.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.medicion.MedicionRepository;
import com.lautaro.multimetrico.medicion.domain.Medicion;
import com.lautaro.multimetrico.medicion.domain.MedicionesVigentes;
import com.lautaro.multimetrico.pliego.domain.Metrica;
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
 * <p>Cada test usa su propio período, como el resto de la suite: el contexto de
 * Spring —y ahora la base— se comparten entre clases y las mediciones se
 * acumulan.
 */
@SpringBootTest
class MedicionEnBaseTest {

    @Autowired
    private MedicionRepository repositorio;

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
        YearMonth periodo = YearMonth.of(2040, 1);

        repositorio.guardar(List.of(medicion(periodo, Metrica.TMO, "690", 22)));
        repositorio.guardar(List.of(medicion(periodo, Metrica.TMO, "612", 22)));

        MedicionesVigentes vigentes = MedicionesVigentes.de(repositorio.historiaDe(periodo, "p100"));

        assertThat(vigentes.de(Metrica.TMO)).get()
                .satisfies(vigente -> assertThat(vigente.valor()).isEqualByComparingTo("612"));
    }

    /** La historia es el orden de carga, no la cronología de los cortes. */
    @Test
    void laHistoriaSaleEnOrdenDeCargaAunqueLosCortesLleguenAlReves() {
        YearMonth periodo = YearMonth.of(2040, 2);

        repositorio.guardar(List.of(medicion(periodo, Metrica.FCR, "78.2", 22)));
        repositorio.guardar(List.of(medicion(periodo, Metrica.FCR, "74.1", 15)));

        assertThat(repositorio.historiaDe(periodo, "p100"))
                .extracting(medicion -> medicion.valor().toPlainString())
                .containsExactly("78.2", "74.1");

        // Y una carga tardía con corte viejo sigue sin desplazar a la vigente.
        assertThat(MedicionesVigentes.de(repositorio.historiaDe(periodo, "p100")).de(Metrica.FCR)).get()
                .satisfies(vigente -> assertThat(vigente.valor()).isEqualByComparingTo("78.2"));
    }

    /** La historia de todo el mes también, no solo la de un asesor. */
    @Test
    void laHistoriaDelMesEnteroTambienRespetaElOrdenDeCarga() {
        YearMonth periodo = YearMonth.of(2040, 3);

        repositorio.guardar(List.of(
                new Medicion("p900", Metrica.NPS, periodo, new BigDecimal("55.4"), periodo.atDay(22), null)));
        repositorio.guardar(List.of(
                new Medicion("p901", Metrica.NPS, periodo, new BigDecimal("61.0"), periodo.atDay(8), null)));

        assertThat(repositorio.historia(periodo))
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
        YearMonth periodo = YearMonth.of(2040, 4);

        repositorio.guardar(List.of(medicion(periodo, Metrica.FCR, "80.04", 22)));

        assertThat(repositorio.historiaDe(periodo, "p100")).singleElement()
                .satisfies(medicion -> assertThat(medicion.valor()).isEqualByComparingTo("80.04"));
    }

    /**
     * El valor vuelve con la escala con la que entró y no con los seis decimales
     * de la columna. No es cosmético: los mensajes que lee una persona se arman
     * con {@code toPlainString()}, y "sobran 12.000000" es otra cosa que "sobran
     * 12".
     */
    @Test
    void elValorVuelveConLaEscalaConLaQueEntroYNoConLaDeLaColumna() {
        YearMonth periodo = YearMonth.of(2040, 5);

        repositorio.guardar(List.of(
                medicion(periodo, Metrica.TMO, "612", 22),
                medicion(periodo, Metrica.FCR, "78.2", 22)));

        assertThat(repositorio.historiaDe(periodo, "p100"))
                .extracting(medicion -> medicion.valor().toPlainString())
                .containsExactly("612", "78.2");
    }

    /** Los archivos de origen escriben el mismo legajo de las dos formas. */
    @Test
    void elLegajoSeBuscaSinDistinguirMayusculasComoEnMemoria() {
        YearMonth periodo = YearMonth.of(2040, 6);

        repositorio.guardar(List.of(
                new Medicion("p777", Metrica.FCR, periodo, new BigDecimal("70"), periodo.atDay(8), null)));

        assertThat(repositorio.historiaDe(periodo, "P777")).hasSize(1);
    }

    /** La medición se guarda con el id de la importación que la trajo, o sin ninguno. */
    @Test
    void laMedicionGuardaDeQueImportacionSalioYTambienQueNoSalioDeNinguna() {
        YearMonth periodo = YearMonth.of(2040, 7);
        LocalDate corte = periodo.atDay(15);

        repositorio.guardar(List.of(
                new Medicion("p100", Metrica.FCR, periodo, new BigDecimal("70"), corte, "imp-1"),
                new Medicion("p100", Metrica.TMO, periodo, new BigDecimal("640"), corte, null)));

        assertThat(repositorio.historiaDe(periodo, "p100"))
                .extracting(Medicion::origenImportacion)
                .containsExactly("imp-1", null);
    }
}
