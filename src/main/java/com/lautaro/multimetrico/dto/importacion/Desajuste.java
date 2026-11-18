package com.lautaro.multimetrico.dto.importacion;

import java.util.List;

public record Desajuste(String donde, String esperaba, String encontro) {

    public static Desajuste hoja(String esperada, List<String> hojasDelLibro) {
        return new Desajuste(
                "hoja",
                "una hoja llamada '%s'".formatted(esperada),
                hojasDelLibro.isEmpty()
                        ? "el libro no tiene hojas"
                        : "el libro tiene: %s".formatted(String.join(", ", hojasDelLibro)));
    }

    public static Desajuste filaDeEncabezados(int esperada, int ultimaFila) {
        return new Desajuste(
                "fila de encabezados",
                "encabezados en la fila %d".formatted(esperada),
                ultimaFila < esperada
                        ? "la hoja termina en la fila %d".formatted(ultimaFila)
                        : "la fila %d está vacía".formatted(esperada));
    }

    public static Desajuste columna(String rol, String esperada, List<String> encabezadosDeLaHoja) {
        return new Desajuste(
                "columna del %s".formatted(rol),
                "un encabezado '%s'".formatted(esperada),
                encabezadosDeLaHoja.isEmpty()
                        ? "la fila de encabezados no tiene ninguno"
                        : "los encabezados son: %s".formatted(String.join(", ", encabezadosDeLaHoja)));
    }

    public static Desajuste columnaRepetida(String rol, String encabezado, List<String> letras) {
        return new Desajuste(
                "columna del %s".formatted(rol),
                "un solo encabezado '%s'".formatted(encabezado),
                "aparece repetido en las columnas %s".formatted(String.join(", ", letras)));
    }

    public static Desajuste archivoIlegible(String detalle) {
        return new Desajuste("archivo", "una planilla .xlsx legible", detalle);
    }

    public static Desajuste padron(Object periodo, String detalle) {
        return new Desajuste(
                "padrón",
                "un padrón cargado para %s, sin el cual no hay nombres que resolver".formatted(periodo),
                detalle);
    }

    @Override
    public String toString() {
        return "%s: esperaba %s, encontré %s".formatted(donde, esperaba, encontro);
    }
}
