package com.lautaro.multimetrico.exception;

import java.time.YearMonth;

public class PliegoYaCargadoException extends RuntimeException {

    public PliegoYaCargadoException(YearMonth periodo, String campania) {
        super("Ya hay un pliego cargado para %s en la campaña %s. Reemplazarlo cambiaría "
                .formatted(periodo, campania)
                + "resultados ya calculados, así que no se pisa desde acá.");
    }
}
