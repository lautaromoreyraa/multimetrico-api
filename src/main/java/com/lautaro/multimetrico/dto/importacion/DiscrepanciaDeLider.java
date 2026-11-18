package com.lautaro.multimetrico.dto.importacion;

public record DiscrepanciaDeLider(
        int numeroDeFila,
        String textoAsesor,
        String legajo,
        String liderDelArchivo,
        String liderDelPadron) {

    public String explicacion() {
        return ("la fila %d dice que '%s' es del equipo de '%s', pero en el padrón el legajo %s está "
                + "con '%s'; se importó con el padrón")
                .formatted(numeroDeFila, textoAsesor, liderDelArchivo, legajo, liderDelPadron);
    }
}
