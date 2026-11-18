package com.lautaro.multimetrico.dto.resolucion;

import java.util.List;

public record AliasConfirmadoResponse(
        String texto,
        String legajo,
        String nombre,
        List<String> aliasDeEsteLegajo) {
}
