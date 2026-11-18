package com.lautaro.multimetrico.dto.seguridad;

import com.lautaro.multimetrico.enumeration.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "el legajo es obligatorio") String legajo,
        @NotBlank(message = "el nombre es obligatorio") String nombre,
        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 8, message = "la contraseña necesita al menos 8 caracteres") String password,
        @NotNull(message = "el rol es obligatorio") Rol rol,
        String lider) {
}
