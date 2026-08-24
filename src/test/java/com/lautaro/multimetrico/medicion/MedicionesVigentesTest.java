package com.lautaro.multimetrico.medicion;

import static com.lautaro.multimetrico.medicion.MedicionesDeEjemplo.medicion;
import static org.assertj.core.api.Assertions.assertThat;

import com.lautaro.multimetrico.medicion.domain.Medicion;
import com.lautaro.multimetrico.medicion.domain.MedicionesVigentes;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MedicionesVigentesTest {

    @Test
    void elValorVigenteEsElDelCorteMasReciente() {
        MedicionesVigentes vigentes = MedicionesVigentes.de(List.of(
                medicion("u100", Metrica.FCR, "70.5", "2026-08-08"),
                medicion("u100", Metrica.FCR, "74.1", "2026-08-15"),
                medicion("u100", Metrica.FCR, "78.2", "2026-08-22")));

        assertThat(vigentes.de(Metrica.FCR)).get()
                .extracting(Medicion::valor)
                .isEqualTo(new BigDecimal("78.2"));
    }

    /**
     * El archivo de la semana pasada puede llegar tarde. Que se cargue después no
     * lo hace más nuevo.
     */
    @Test
    void unaCargaTardiaConCorteViejoNoDesplazaALaVigente() {
        MedicionesVigentes vigentes = MedicionesVigentes.de(List.of(
                medicion("u100", Metrica.FCR, "78.2", "2026-08-22"),
                medicion("u100", Metrica.FCR, "74.1", "2026-08-15")));

        assertThat(vigentes.de(Metrica.FCR)).get()
                .extracting(Medicion::valor)
                .isEqualTo(new BigDecimal("78.2"));
    }

    /**
     * Dos cargas del mismo corte son el mismo archivo subido dos veces, y la
     * segunda es la corrección. Quedarse con la primera dejaría sin efecto la
     * corrección.
     */
    @Test
    void conElMismoCorteGanaLaUltimaCargada() {
        MedicionesVigentes vigentes = MedicionesVigentes.de(List.of(
                medicion("u100", Metrica.TMO, "690", "2026-08-22"),
                medicion("u100", Metrica.TMO, "612", "2026-08-22")));

        assertThat(vigentes.de(Metrica.TMO)).get()
                .extracting(Medicion::valor)
                .isEqualTo(new BigDecimal("612"));
    }

    @Test
    void cadaMetricaConservaSuPropiaFechaDeCorte() {
        MedicionesVigentes vigentes = MedicionesVigentes.de(List.of(
                medicion("u100", Metrica.TMO, "612", "2026-08-22"),
                medicion("u100", Metrica.FCR, "78.2", "2026-08-15")));

        assertThat(vigentes.corteDe(Metrica.TMO)).contains(LocalDate.parse("2026-08-22"));
        assertThat(vigentes.corteDe(Metrica.FCR)).contains(LocalDate.parse("2026-08-15"));
    }

    /** Una métrica sin medición está faltante: no aparece, y no aparece en cero. */
    @Test
    void unaMetricaSinMedicionNoApareceEnLosValores() {
        MedicionesVigentes vigentes = MedicionesVigentes.de(List.of(
                medicion("u100", Metrica.TMO, "612", "2026-08-22")));

        assertThat(vigentes.valores()).containsOnlyKeys(Metrica.TMO);
        assertThat(vigentes.de(Metrica.NPS)).isEmpty();
        assertThat(vigentes.corteDe(Metrica.NPS)).isEmpty();
    }

    @Test
    void sinHistoriaNoHayNadaVigente() {
        assertThat(MedicionesVigentes.de(List.of()).vacia()).isTrue();
        assertThat(MedicionesVigentes.ninguna().valores()).isEmpty();
    }
}
