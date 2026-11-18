package com.lautaro.multimetrico.service.medicion;

import static com.lautaro.multimetrico.fixture.MedicionesDeEjemplo.medicion;
import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.dto.medicion.MedicionVigente;
import com.lautaro.multimetrico.dto.medicion.MedicionesVigentes;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VigenciaDeMedicionesTest {

    @Autowired
    private MedicionService servicio;

    private MedicionesVigentes vigentesTras(YearMonth periodo, String... valorYCorte) {
        for (int i = 0; i < valorYCorte.length; i += 3) {
            servicio.registrar(List.of(medicion(periodo, "v100",
                    Metrica.valueOf(valorYCorte[i]), valorYCorte[i + 1], valorYCorte[i + 2])));
        }
        return servicio.vigentesDe(periodo, "v100");
    }

    @Test
    void elValorVigenteEsElDelCorteMasReciente() {
        MedicionesVigentes vigentes = vigentesTras(YearMonth.of(2041, 1),
                "FCR", "70.5", "2041-01-08",
                "FCR", "74.1", "2041-01-15",
                "FCR", "78.2", "2041-01-22");

        assertThat(vigentes.de(Metrica.FCR)).get()
                .extracting(MedicionVigente::valor)
                .isEqualTo(new BigDecimal("78.2"));
    }

    @Test
    void unaCargaTardiaConCorteViejoNoDesplazaALaVigente() {
        MedicionesVigentes vigentes = vigentesTras(YearMonth.of(2041, 2),
                "FCR", "78.2", "2041-02-22",
                "FCR", "74.1", "2041-02-15");

        assertThat(vigentes.de(Metrica.FCR)).get()
                .extracting(MedicionVigente::valor)
                .isEqualTo(new BigDecimal("78.2"));
    }

    @Test
    void conElMismoCorteGanaLaUltimaCargada() {
        MedicionesVigentes vigentes = vigentesTras(YearMonth.of(2041, 3),
                "TMO", "690", "2041-03-22",
                "TMO", "612", "2041-03-22");

        assertThat(vigentes.de(Metrica.TMO)).get()
                .extracting(MedicionVigente::valor)
                .isEqualTo(new BigDecimal("612"));
    }

    @Test
    void cadaMetricaConservaSuPropiaFechaDeCorte() {
        MedicionesVigentes vigentes = vigentesTras(YearMonth.of(2041, 4),
                "TMO", "612", "2041-04-22",
                "FCR", "78.2", "2041-04-15");

        assertThat(vigentes.corteDe(Metrica.TMO)).contains(LocalDate.parse("2041-04-22"));
        assertThat(vigentes.corteDe(Metrica.FCR)).contains(LocalDate.parse("2041-04-15"));
    }

    @Test
    void unaMetricaSinMedicionNoApareceEnLosValores() {
        MedicionesVigentes vigentes = vigentesTras(YearMonth.of(2041, 5),
                "TMO", "612", "2041-05-22");

        assertThat(vigentes.valores()).containsOnlyKeys(Metrica.TMO);
        assertThat(vigentes.de(Metrica.NPS)).isEmpty();
        assertThat(vigentes.corteDe(Metrica.NPS)).isEmpty();
    }

    @Test
    void sinHistoriaNoHayNadaVigente() {
        assertThat(servicio.vigentesDe(YearMonth.of(2041, 6), "v100").vacia()).isTrue();
        assertThat(MedicionesVigentes.ninguna().valores()).isEmpty();
    }
}
