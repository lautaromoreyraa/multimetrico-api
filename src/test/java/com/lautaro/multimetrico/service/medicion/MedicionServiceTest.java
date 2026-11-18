package com.lautaro.multimetrico.service.medicion;

import static com.lautaro.multimetrico.fixture.MedicionesDeEjemplo.medicion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.dto.medicion.MedicionVigente;
import com.lautaro.multimetrico.dto.medicion.MedicionesVigentes;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MedicionServiceTest {

    @Autowired
    private MedicionService servicio;

    @Test
    void cadaCargaSemanalAgregaEnVezDePisarLaAnterior() {
        YearMonth periodo = YearMonth.of(2040, 1);

        servicio.registrar(List.of(medicion(periodo, "s100", Metrica.FCR, "70.5", "2040-01-08")));
        servicio.registrar(List.of(medicion(periodo, "s100", Metrica.FCR, "74.1", "2040-01-15")));
        servicio.registrar(List.of(medicion(periodo, "s100", Metrica.FCR, "78.2", "2040-01-22")));

        assertThat(servicio.historia(periodo, "s100", Metrica.FCR))
                .extracting(Medicion::valor)
                .containsExactly(new BigDecimal("70.5"), new BigDecimal("74.1"), new BigDecimal("78.2"));

        assertThat(servicio.vigentesDe(periodo, "s100").de(Metrica.FCR)).get()
                .extracting(MedicionVigente::valor)
                .isEqualTo(new BigDecimal("78.2"));
    }

    @Test
    void laHistoriaSaleEnOrdenDeCargaYNoOrdenadaPorCorte() {
        YearMonth periodo = YearMonth.of(2040, 2);

        servicio.registrar(List.of(
                medicion(periodo, "s100", Metrica.FCR, "78.2", "2040-02-22"),
                medicion(periodo, "s100", Metrica.FCR, "74.1", "2040-02-15")));

        assertThat(servicio.historia(periodo, "s100", Metrica.FCR))
                .extracting(Medicion::valor)
                .containsExactly(new BigDecimal("78.2"), new BigDecimal("74.1"));
    }

    @Test
    void laHistoriaSeFiltraPorAsesorYPorMetrica() {
        YearMonth periodo = YearMonth.of(2040, 3);

        servicio.registrar(List.of(
                medicion(periodo, "s100", Metrica.FCR, "78.2", "2040-03-22"),
                medicion(periodo, "s100", Metrica.TMO, "612", "2040-03-22"),
                medicion(periodo, "s200", Metrica.FCR, "61.0", "2040-03-22")));

        assertThat(servicio.historia(periodo, null, null)).hasSize(3);
        assertThat(servicio.historia(periodo, "s100", null)).hasSize(2);
        assertThat(servicio.historia(periodo, null, Metrica.FCR)).hasSize(2);
        assertThat(servicio.historia(periodo, "s100", Metrica.FCR)).hasSize(1);
    }

    @Test
    void elLegajoSeBuscaSinDistinguirMayusculas() {
        YearMonth periodo = YearMonth.of(2040, 4);

        servicio.registrar(List.of(medicion(periodo, "s100", Metrica.FCR, "78.2", "2040-04-22")));

        assertThat(servicio.historia(periodo, "S100", null)).hasSize(1);
        assertThat(servicio.vigentesDe(periodo, "S100").vacia()).isFalse();
    }

    @Test
    void cadaPeriodoTieneSuPropiaHistoria() {
        YearMonth periodo = YearMonth.of(2040, 5);

        servicio.registrar(List.of(medicion(periodo, "s100", Metrica.FCR, "78.2", "2040-05-22")));

        assertThat(servicio.historia(YearMonth.of(2040, 6), null, null)).isEmpty();
    }

    @Test
    void agrupaLasVigentesDeTodaLaOperacionEnUnaSolaPasada() {
        YearMonth periodo = YearMonth.of(2040, 7);

        servicio.registrar(List.of(
                medicion(periodo, "s100", Metrica.FCR, "70.5", "2040-07-15"),
                medicion(periodo, "s100", Metrica.FCR, "78.2", "2040-07-22"),
                medicion(periodo, "s200", Metrica.FCR, "61.0", "2040-07-15")));

        Map<String, MedicionesVigentes> vigentes = servicio.vigentesDelPeriodo(periodo);

        assertThat(vigentes).containsOnlyKeys("S100", "S200");
        assertThat(vigentes.get("S100").valores().get(Metrica.FCR)).isEqualByComparingTo("78.2");
        assertThat(vigentes.get("S200").valores().get(Metrica.FCR)).isEqualByComparingTo("61.0");
    }

    @Test
    void marcaComoVigenteUnaSolaFilaPorAsesorYMetrica() {
        YearMonth periodo = YearMonth.of(2040, 8);

        servicio.registrar(List.of(
                medicion(periodo, "s100", Metrica.TMO, "612", "2040-08-22"),
                medicion(periodo, "s100", Metrica.TMO, "612", "2040-08-22")));

        List<Medicion> historia = servicio.historia(periodo, "s100", Metrica.TMO);

        assertThat(historia).hasSize(2);
        assertThat(servicio.vigentesEn(historia)).containsExactly(historia.getLast().id());
    }

    @Test
    void unLoteVacioNoEsUnAltaSilenciosa() {
        assertThatThrownBy(() -> servicio.registrar(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ninguna medición");
    }
}
