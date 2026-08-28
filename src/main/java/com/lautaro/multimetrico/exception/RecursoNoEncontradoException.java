package com.lautaro.multimetrico.exception;

/**
 * Se pidió algo que no está cargado: un pliego, un padrón, un perfil, una
 * importación, un legajo.
 *
 * <p>Reemplaza al {@code NoSuchElementException} que este proyecto usaba para lo
 * mismo. La diferencia importa: {@code NoSuchElementException} también lo lanza
 * un {@code Optional.get()} o un iterador vacío, así que el manejador global no
 * podía distinguir "no hay pliego para agosto" —un 404 legítimo— de un bug
 * interno, y devolvía 404 para los dos. Un error de programación disfrazado de
 * "no encontrado" es un error que nadie va a mirar.
 *
 * <p>El mensaje se escribe para que lo lea una persona y, cuando se puede, dice
 * qué sí existe: pedir el equipo de un líder que no está en el padrón devuelve la
 * lista de los que sí, porque con eso quien lo lee ve el suyo o entiende por qué
 * no está.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
