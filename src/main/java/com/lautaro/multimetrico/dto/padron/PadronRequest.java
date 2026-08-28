package com.lautaro.multimetrico.dto.padron;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.YearMonth;
import java.util.List;

/** El padrón de un período como llega por HTTP: la nómina entera, con su líder. */
public record PadronRequest(
        @NotNull(message = "el período es obligatorio (formato aaaa-mm)") YearMonth periodo,
        @NotEmpty(message = "el padrón necesita al menos un asesor") @Valid List<AsesorRequest> asesores) {

    public record AsesorRequest(
            @NotBlank(message = "el legajo es obligatorio") String legajo,
            @NotBlank(message = "el nombre es obligatorio") String nombre,
            @NotBlank(message = "el líder es obligatorio") String lider) {
    }
}
