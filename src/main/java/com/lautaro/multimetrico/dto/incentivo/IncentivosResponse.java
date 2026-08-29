package com.lautaro.multimetrico.dto.incentivo;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Los incentivos de un equipo en un mes.
 *
 * <p>Se recorre <b>del padrón hacia los incentivos</b> y no al revés, igual que
 * el multimétrico: un asesor que no vino en la planilla aparece igual, con el
 * importe vacío, en vez de desaparecer de la lista. Que alguien falte es
 * exactamente lo que hay que poder ver, y es lo que la planilla original esconde.
 *
 * @param sinImporte cuántos de esos asesores no vinieron en el archivo
 * @param total la suma de lo que sí vino. No se completa con ceros: un total de
 *     dos millones con tres personas sin cargar no es lo mismo que uno completo
 */
public record IncentivosResponse(
        YearMonth periodo,
        String lider,
        int asesores,
        int sinImporte,
        BigDecimal total,
        List<FilaDeIncentivo> filas) {

    public IncentivosResponse {
        filas = List.copyOf(filas);
    }
}
