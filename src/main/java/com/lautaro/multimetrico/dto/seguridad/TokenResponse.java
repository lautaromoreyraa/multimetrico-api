package com.lautaro.multimetrico.dto.seguridad;

import com.lautaro.multimetrico.enumeration.Rol;
import java.time.Instant;

/**
 * El token y lo mínimo para pintar la pantalla sin volver a preguntar.
 *
 * <p>Van el nombre, el rol y el líder porque el cliente los necesita apenas
 * entra —para saber si muestra la tabla del equipo o una sola fila— y sacarlos de
 * acá le ahorra un request. Están adentro del token igual, pero un cliente no
 * tiene por qué decodificarlo: eso lo ataría al formato interno.
 *
 * <p>{@code vence} viaja para que la pantalla pueda avisar antes de que el token
 * muera, en vez de enterarse con un 401 en medio de una carga.
 */
public record TokenResponse(
        String token,
        String legajo,
        String nombre,
        Rol rol,
        String lider,
        Instant vence) {
}
