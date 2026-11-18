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

@SpringBootTest
class MedicionRepositoryTest {

    @Autowired
    private MedicionRepository repositorio;

    @Autowired
    private MedicionService mediciones;

    private static Medicion medicion(YearMonth periodo, Metrica metrica, String valor, int diaDeCorte) {
        return new Medicion("p100", metrica, periodo, new BigDecimal(valor), periodo.atDay(diaDeCorte), null);
    }

    @Test
    void conElMismoCorteGanaLaUltimaCargadaTambienCuandoVieneDeLaBase() {
        YearMonth periodo = YearMonth.of(2045, 1);

        repositorio.saveAll(List.of(medicion(periodo, Metrica.TMO, "690", 22)));
        repositorio.saveAll(List.of(medicion(periodo, Metrica.TMO, "612", 22)));

        MedicionesVigentes vigentes = mediciones.vigentesDe(periodo, "p100");

        assertThat(vigentes.de(Metrica.TMO)).get()
                .satisfies(vigente -> assertThat(vigente.valor()).isEqualByComparingTo("612"));
    }

    @Test
    void laHistoriaSaleEnOrdenDeCargaAunqueLosCortesLleguenAlReves() {
        YearMonth periodo = YearMonth.of(2045, 2);

        repositorio.saveAll(List.of(medicion(periodo, Metrica.FCR, "78.2", 22)));
        repositorio.saveAll(List.of(medicion(periodo, Metrica.FCR, "74.1", 15)));

        assertThat(repositorio.findByPeriodoAndLegajoClaveOrderByIdAsc(periodo, Medicion.clave("p100")))
                .extracting(medicion -> medicion.valor().toPlainString())
                .containsExactly("78.2", "74.1");

        assertThat(mediciones.vigentesDe(periodo, "p100").de(Metrica.FCR)).get()
                .satisfies(vigente -> assertThat(vigente.valor()).isEqualByComparingTo("78.2"));
    }

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

    @Test
    void unValorConDecimalesNoSePierdeAlGuardarlo() {
        YearMonth periodo = YearMonth.of(2045, 4);

        repositorio.saveAll(List.of(medicion(periodo, Metrica.FCR, "80.04", 22)));

        assertThat(repositorio.findByPeriodoAndLegajoClaveOrderByIdAsc(periodo, Medicion.clave("p100")))
                .singleElement()
                .satisfies(medicion -> assertThat(medicion.valor()).isEqualByComparingTo("80.04"));
    }

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

    @Test
    void elLegajoSeBuscaSinDistinguirMayusculas() {
        YearMonth periodo = YearMonth.of(2045, 6);

        repositorio.saveAll(List.of(
                new Medicion("p777", Metrica.FCR, periodo, new BigDecimal("70"), periodo.atDay(8), null)));

        assertThat(repositorio.findByPeriodoAndLegajoClaveOrderByIdAsc(periodo, Medicion.clave("P777")))
                .hasSize(1);
    }

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

    @Test
    void laMedicionGuardadaVuelveConSuIdAsignado() {
        YearMonth periodo = YearMonth.of(2045, 8);

        List<Medicion> guardadas = repositorio.saveAll(List.of(medicion(periodo, Metrica.NPS, "55.4", 22)));

        assertThat(guardadas).singleElement()
                .satisfies(medicion -> assertThat(medicion.id()).isNotNull());
    }
}
