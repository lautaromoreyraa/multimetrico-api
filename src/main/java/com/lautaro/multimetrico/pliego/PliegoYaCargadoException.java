package com.lautaro.multimetrico.pliego;

import java.time.YearMonth;

/**
 * Un pliego ya cargado no se pisa.
 *
 * <p>Es el punto entero del proyecto: si el pliego de marzo se puede
 * sobrescribir, el cálculo de marzo deja de ser reproducible y volvemos a la
 * planilla. Corregir uno es una operación aparte y deliberada, no el efecto
 * secundario de volver a subir un archivo.
 */
public class PliegoYaCargadoException extends RuntimeException {

    public PliegoYaCargadoException(YearMonth periodo, String campania) {
        super("Ya hay un pliego cargado para %s en la campaña %s. Reemplazarlo cambiaría "
                .formatted(periodo, campania)
                + "resultados ya calculados, así que no se pisa desde acá.");
    }
}
