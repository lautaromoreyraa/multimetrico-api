package com.lautaro.multimetrico.dto.seguridad;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "el legajo es obligatorio") String legajo,
        @NotBlank(message = "la contraseña es obligatoria") String password) {
}
