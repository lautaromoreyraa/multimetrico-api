package com.lautaro.multimetrico.exception;

import com.lautaro.multimetrico.dto.pliego.Violacion;
import java.util.List;

/**
 * Se lanza cuando un pliego no pasa la validación.
 *
 * <p>Lleva <b>todas</b> las violaciones, no la primera: quien carga el pliego del
 * mes tiene que poder corregirlo de una sola pasada, no descubrir el siguiente
 * problema recién después de arreglar el anterior.
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
