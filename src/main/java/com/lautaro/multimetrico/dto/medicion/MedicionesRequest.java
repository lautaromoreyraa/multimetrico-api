package com.lautaro.multimetrico.dto.medicion;

import com.lautaro.multimetrico.enumeration.Metrica;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record MedicionesRequest(
        @NotNull(message = "el período es obligatorio (formato aaaa-mm)") YearMonth periodo,
        @NotNull(message = "la fecha de corte es obligatoria (formato aaaa-mm-dd)") LocalDate fechaDeCorte,
        String origenImportacion,
        @NotEmpty(message = "el lote necesita al menos una medición") @Valid List<ItemRequest> mediciones) {

    public record ItemRequest(
            @NotBlank(message = "el legajo es obligatorio") String legajo,
            @NotNull(message = "la métrica es obligatoria") Metrica metrica,
            @NotNull(message = "el valor es obligatorio; una métrica sin dato no se carga") BigDecimal valor,
            LocalDate fechaDeCorte) {
    }
}
