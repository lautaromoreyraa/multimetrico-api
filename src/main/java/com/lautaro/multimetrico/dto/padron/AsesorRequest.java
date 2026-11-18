package com.lautaro.multimetrico.dto.padron;

import jakarta.validation.constraints.NotBlank;

public record AsesorRequest(
        @NotBlank(message = "el legajo es obligatorio") String legajo,
        @NotBlank(message = "el nombre es obligatorio") String nombre,
        @NotBlank(message = "el líder es obligatorio") String lider) {
}
