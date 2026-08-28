package com.lautaro.multimetrico.dto.multimetrico;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * El multimétrico de un equipo: una fila por asesor, más lo que hace falta para
 * leerlas juntas.
 *
 * <p>El {@code lider} puede venir en {@code null}, y ahí son todos los asesores
 * del período. Es la vista que hoy no existe: cada supervisor arma la suya
 * filtrando el archivo por su propio nombre, así que nadie ve la operación
 * entera con los mismos criterios aplicados.
 *
 * <p>El {@code alcanceMaximo} viene del pliego y no de las filas. Sin él, un
 * total de 9,5 no significa nada; con él se sabe si es 9,5 sobre 12 o sobre 20.
 */
public record Multimetrico(
        YearMonth periodo,
        String campania,
        String lider,
        BigDecimal alcanceMaximo,
        ResumenDeEquipo resumen,
        List<FilaDeMultimetrico> filas) {

    public Multimetrico {
        filas = List.copyOf(filas);
    }
}
