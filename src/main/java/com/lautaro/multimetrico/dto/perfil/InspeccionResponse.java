package com.lautaro.multimetrico.dto.perfil;

import java.util.List;

public record InspeccionResponse(
        List<String> hojas,
        int filaEncabezados,
        List<ColumnaResponse> columnas) {

    public record ColumnaResponse(String encabezado, List<String> ejemplos) {
    }
}
