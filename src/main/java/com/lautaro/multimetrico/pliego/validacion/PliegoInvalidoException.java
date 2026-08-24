package com.lautaro.multimetrico.pliego.validacion;

import java.util.List;

/**
 * Se lanza cuando un pliego no pasa la validación. Lleva todas las violaciones,
 * no la primera: quien carga el pliego tiene que poder corregirlo de una sola
 * pasada.
 */
public class PliegoInvalidoException extends RuntimeException {

    private final transient List<Violacion> violaciones;

    public PliegoInvalidoException(List<Violacion> violaciones) {
        super("El pliego tiene %d problema(s) que impiden usarlo".formatted(violaciones.size()));
        this.violaciones = List.copyOf(violaciones);
    }

    public List<Violacion> violaciones() {
        return violaciones;
    }
}
