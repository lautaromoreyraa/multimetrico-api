package com.lautaro.multimetrico.dto.importacion;

import java.util.List;

/**
 * Algo del archivo que no coincide con el perfil, dicho como "esperaba esto,
 * encontré esto otro".
 *
 * <p>Un desajuste no rechaza una fila: rechaza la <b>importación entera</b>. La
 * distinción es todo el punto. Si cambió un encabezado y el sistema siguiera
 * adelante, leería la columna de al lado y guardaría números creíbles y
 * equivocados para todo el equipo, que es lo que hace la planilla y por lo que
 * nadie se entera. Frenar del todo es la respuesta correcta: el archivo se
 * volverá a subir cuando el perfil esté corregido, y mientras tanto no hay datos
 * malos dando vueltas.
 *
 * <p>El {@code encontro} lista lo que sí había, y eso no es cortesía: es lo
 * que le permite a quien arma el perfil corregirlo sin abrir el archivo. Por la
 * misma razón la lectura junta <b>todos</b> los desajustes antes de fallar en
 * vez de cortar en el primero, igual que hace el validador del pliego.
 */
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

    /**
     * El mismo encabezado dos veces también frena la importación.
     *
     * <p>Elegir la primera de las dos columnas sería la decisión silenciosa de
     * siempre: funciona hasta el día en que la buena es la segunda.
     */
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
