package com.lautaro.multimetrico.dto.pliego;

import com.lautaro.multimetrico.enumeration.Metrica;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record PliegoRequest(
        @NotNull(message = "el período es obligatorio (formato aaaa-mm)") YearMonth periodo,
        @NotBlank(message = "la campaña es obligatoria") String campania,
        @NotEmpty(message = "el pliego necesita al menos una escala") @Valid List<EscalaRequest> escalas) {

    public record EscalaRequest(
            @NotNull(message = "la métrica es obligatoria") Metrica metrica,
            @NotNull(message = "el peso es obligatorio") BigDecimal peso,
            @NotEmpty(message = "la escala necesita bandas") @Valid List<BandaRequest> bandas) {
    }

    public record BandaRequest(
            BigDecimal desde,
            BigDecimal hasta,
            @NotNull(message = "la banda necesita un resultado") BigDecimal resultado) {
    }
}
