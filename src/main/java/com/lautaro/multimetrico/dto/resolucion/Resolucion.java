package com.lautaro.multimetrico.dto.resolucion;

import java.util.List;

public record Resolucion(List<Coincidencia> resueltos, List<Pendiente> pendientes, boolean completa) {

    public Resolucion {
        resueltos = List.copyOf(resueltos);
        pendientes = List.copyOf(pendientes);
    }

    public static Resolucion de(List<Coincidencia> resueltos, List<Pendiente> pendientes) {
        return new Resolucion(resueltos, pendientes, pendientes.isEmpty());
    }
}
