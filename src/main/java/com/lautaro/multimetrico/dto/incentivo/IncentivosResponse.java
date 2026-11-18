package com.lautaro.multimetrico.dto.incentivo;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

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
