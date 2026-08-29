package com.lautaro.multimetrico.exception;

/**
 * El login no coincide.
 *
 * <p>Una sola excepción para "el legajo no existe", "la contraseña está mal" y
 * "el usuario está desactivado", a propósito: distinguirlas en la respuesta
 * convertiría al login en una forma de averiguar qué legajos existen. Ver
 * {@code UsuarioServiceImpl}.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
