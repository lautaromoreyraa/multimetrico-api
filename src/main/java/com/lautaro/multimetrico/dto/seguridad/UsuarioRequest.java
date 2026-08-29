package com.lautaro.multimetrico.dto.seguridad;

import com.lautaro.multimetrico.enumeration.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * El alta de un usuario, que sólo hace un administrador.
 *
 * <p>No hay registro público: quién entra a ver cuánto cobra la gente no es algo
 * que se resuelva con un formulario abierto.
 *
 * <p>El mínimo de ocho caracteres es el piso de cualquier recomendación vigente.
 * No se exige mezcla de mayúsculas y símbolos a propósito: esa regla empuja a
 * contraseñas cortas y con sustituciones predecibles, que son más fáciles de
 * romper que una frase larga.
 */
public record UsuarioRequest(
        @NotBlank(message = "el legajo es obligatorio") String legajo,
        @NotBlank(message = "el nombre es obligatorio") String nombre,
        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 8, message = "la contraseña necesita al menos 8 caracteres") String password,
        @NotNull(message = "el rol es obligatorio") Rol rol,
        String lider) {
}
