package com.lautaro.multimetrico.medicion;

import static com.lautaro.multimetrico.medicion.MedicionesDeEjemplo.AGOSTO;
import static com.lautaro.multimetrico.medicion.MedicionesDeEjemplo.medicion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lautaro.multimetrico.medicion.domain.Medicion;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class MedicionServiceTest {

    private final MedicionService servicio = new MedicionService(new MedicionEnMemoria());

    /**
     * El defecto que este módulo corrige: en la planilla la carga del lunes pisa
     * la del lunes anterior y el número viejo deja de existir.
     */
    @Test
    void cadaCargaSemanalAgregaEnVezDePisarLaAnterior() {
        servicio.registrar(List.of(medicion("u100", Metrica.FCR, "70.5", "2026-08-08")));
        servicio.registrar(List.of(medicion("u100", Metrica.FCR, "74.1", "2026-08-15")));
        servicio.registrar(List.of(medicion("u100", Metrica.FCR, "78.2", "2026-08-22")));

        assertThat(servicio.historia(AGOSTO, "u100", Metrica.FCR))
                .extracting(Medicion::valor)
                .containsExactly(new BigDecimal("70.5"), new BigDecimal("74.1"), new BigDecimal("78.2"));

        assertThat(servicio.vigentesDe(AGOSTO, "u100").de(Metrica.FCR)).get()
                .extracting(Medicion::valor)
                .isEqualTo(new BigDecimal("78.2"));
    }

    @Test
    void laHistoriaSaleEnOrdenDeCargaYNoOrdenadaPorCorte() {
        servicio.registrar(List.of(
                medicion("u100", Metrica.FCR, "78.2", "2026-08-22"),
                medicion("u100", Metrica.FCR, "74.1", "2026-08-15")));

        assertThat(servicio.historia(AGOSTO, "u100", Metrica.FCR))
                .extracting(Medicion::valor)
                .containsExactly(new BigDecimal("78.2"), new BigDecimal("74.1"));
    }

    @Test
    void laHistoriaSeFiltraPorAsesorYPorMetrica() {
        servicio.registrar(List.of(
                medicion("u100", Metrica.FCR, "78.2", "2026-08-22"),
                medicion("u100", Metrica.TMO, "612", "2026-08-22"),
                medicion("u200", Metrica.FCR, "61.0", "2026-08-22")));

        assertThat(servicio.historia(AGOSTO, null, null)).hasSize(3);
        assertThat(servicio.historia(AGOSTO, "u100", null)).hasSize(2);
        assertThat(servicio.historia(AGOSTO, null, Metrica.FCR)).hasSize(2);
        assertThat(servicio.historia(AGOSTO, "u100", Metrica.FCR)).hasSize(1);
    }

    /** Los archivos de origen escriben el mismo legajo de las dos formas. */
    @Test
    void elLegajoSeBuscaSinDistinguirMayusculas() {
        servicio.registrar(List.of(medicion("u100", Metrica.FCR, "78.2", "2026-08-22")));

        assertThat(servicio.historia(AGOSTO, "U100", null)).hasSize(1);
        assertThat(servicio.vigentesDe(AGOSTO, "U100").vacia()).isFalse();
    }

    @Test
    void cadaPeriodoTieneSuPropiaHistoria() {
        servicio.registrar(List.of(medicion("u100", Metrica.FCR, "78.2", "2026-08-22")));

        assertThat(servicio.historia(YearMonth.of(2026, 9), null, null)).isEmpty();
    }

    @Test
    void agrupaLasVigentesDeTodaLaOperacionEnUnaSolaPasada() {
        servicio.registrar(List.of(
                medicion("u100", Metrica.FCR, "70.5", "2026-08-15"),
                medicion("u100", Metrica.FCR, "78.2", "2026-08-22"),
                medicion("u200", Metrica.FCR, "61.0", "2026-08-15")));

        var vigentes = servicio.vigentesDelPeriodo(AGOSTO);

        assertThat(vigentes).containsOnlyKeys("U100", "U200");
        assertThat(vigentes.get("U100").valores()).containsEntry(Metrica.FCR, new BigDecimal("78.2"));
        assertThat(vigentes.get("U200").valores()).containsEntry(Metrica.FCR, new BigDecimal("61.0"));
    }

    @Test
    void unLoteVacioNoEsUnAltaSilenciosa() {
        assertThatThrownBy(() -> servicio.registrar(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ninguna medición");
    }
}
