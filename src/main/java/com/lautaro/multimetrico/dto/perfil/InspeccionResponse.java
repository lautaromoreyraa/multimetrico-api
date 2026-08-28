package com.lautaro.multimetrico.dto.perfil;

import java.util.List;

/**
 * Las columnas de un archivo con valores de ejemplo.
 *
 * <p>Es el arranque cuando llega una fuente nueva: el sistema <b>no adivina</b>
 * qué columna es cuál. Muestra lo que hay —los encabezados y las primeras filas
 * de cada uno— y una persona arma el perfil mirándolo. Adivinar sería la decisión
 * silenciosa de siempre: acierta hasta el día que no, y ese día importa la
 * columna de al lado sin decir nada.
 *
 * <p>{@code hojas} viene aunque no se haya pedido ninguna en particular: si el
 * libro tiene cincuenta hojas —y el archivo real las tiene—, lo primero que hay
 * que elegir es cuál.
 */
public record InspeccionResponse(
        List<String> hojas,
        int filaEncabezados,
        List<ColumnaResponse> columnas) {

    public record ColumnaResponse(String encabezado, List<String> ejemplos) {
    }
}
