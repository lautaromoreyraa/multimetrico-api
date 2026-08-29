package com.lautaro.multimetrico.exception;

/**
 * Quien pide está autenticado pero el dato no es suyo.
 *
 * <p>Es un 403 y no un 404. Se consideró devolver 404 para no confirmar que ese
 * legajo existe, pero acá no aporta: el padrón del equipo ya es visible para
 * quien tiene acceso, y para un asesor cualquier legajo que no sea el suyo da lo
 * mismo. Un 403 explica lo que pasó; un 404 mentiría y mandaría a buscar el
 * problema al lado equivocado, que es exactamente lo que este proyecto evita en
 * el resto de sus errores.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
