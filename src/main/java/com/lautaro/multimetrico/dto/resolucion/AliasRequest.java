package com.lautaro.multimetrico.dto.resolucion;

import jakarta.validation.constraints.NotBlank;

/**
 * Una persona confirma que un texto corresponde a un legajo.
 *
 * <p>El {@code texto} va tal como venía en el archivo, sin limpiar: normalizarlo
 * es trabajo del servidor, y mandarlo ya normalizado desde el cliente haría que
 * dos clientes con criterios distintos guarden dos alias para el mismo nombre.
 */
public record AliasRequest(
        @NotBlank(message = "el texto es obligatorio") String texto,
        @NotBlank(message = "el legajo es obligatorio") String legajo) {
}
