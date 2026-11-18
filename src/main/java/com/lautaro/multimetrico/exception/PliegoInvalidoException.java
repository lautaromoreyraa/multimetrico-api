package com.lautaro.multimetrico.exception;

import com.lautaro.multimetrico.dto.pliego.Violacion;
import java.util.List;

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
