package com.lautaro.multimetrico.dto.pliego;

import com.lautaro.multimetrico.enumeration.Metrica;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * El pliego como llega por HTTP.
 *
 * <p>Los DTOs se quedan en el borde: la entidad no sabe que existe JSON. Los
 * extremos abiertos viajan como {@code null} —{@code desde: null} es "sin
 * mínimo"—, que es la forma honesta de decirlo. La planilla usa el texto
 * "Máximo" en una tabla y el número -1 en otra para lo mismo; traducir esas dos
 * convenciones a null es trabajo del importador, no del modelo.
 */
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
