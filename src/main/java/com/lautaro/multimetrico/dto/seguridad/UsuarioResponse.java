package com.lautaro.multimetrico.dto.seguridad;

import com.lautaro.multimetrico.enumeration.Rol;

/**
 * Un usuario tal como sale por HTTP.
 *
 * <p>Sin la contraseña, obviamente, y por eso es un record aparte de la entidad:
 * un DTO que se arma campo por campo no puede filtrar un campo nuevo por olvido,
 * que es lo que pasa cuando se serializa la entidad directo.
 */
public record UsuarioResponse(String legajo, String nombre, Rol rol, String lider, boolean activo) {
}
