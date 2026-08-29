package com.lautaro.multimetrico.dto.seguridad;

import jakarta.validation.constraints.NotBlank;

/**
 * Lo que manda quien quiere entrar.
 *
 * <p>El legajo y no un correo: es la identidad que ya usa todo el resto del
 * sistema, y agregar una segunda obligaría a traducir en cada control de acceso.
 */
public record LoginRequest(
        @NotBlank(message = "el legajo es obligatorio") String legajo,
        @NotBlank(message = "la contraseña es obligatoria") String password) {
}
