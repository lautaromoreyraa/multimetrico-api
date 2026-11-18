package com.lautaro.multimetrico.dto.pliego;

import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.Unidad;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record PliegoResponse(
        YearMonth periodo,
        String campania,
        BigDecimal alcanceMaximo,
        List<EscalaResponse> escalas) {

    public record EscalaResponse(
            Metrica metrica,
            String descripcion,
            String sentido,
            Unidad unidad,
            BigDecimal peso,
            List<BandaResponse> bandas) {
    }

    public record BandaResponse(BigDecimal desde, BigDecimal hasta, BigDecimal resultado, String rango) {
    }
}
