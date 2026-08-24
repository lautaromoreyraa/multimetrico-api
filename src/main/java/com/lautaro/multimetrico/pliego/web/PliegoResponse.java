package com.lautaro.multimetrico.pliego.web;

import com.lautaro.multimetrico.pliego.domain.Escala;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import com.lautaro.multimetrico.pliego.domain.Pliego;
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
            BigDecimal peso,
            List<BandaResponse> bandas) {
    }

    public record BandaResponse(BigDecimal desde, BigDecimal hasta, BigDecimal resultado, String rango) {
    }

    public static PliegoResponse de(Pliego pliego) {
        return new PliegoResponse(
                pliego.periodo(),
                pliego.campania(),
                pliego.alcanceMaximo(),
                pliego.escalas().stream().map(PliegoResponse::escala).toList());
    }

    private static EscalaResponse escala(Escala escala) {
        return new EscalaResponse(
                escala.metrica(),
                escala.metrica().descripcion(),
                escala.metrica().sentido().name(),
                escala.peso(),
                // Se devuelven ordenadas: el que las lee espera una tabla, no el
                // orden en que quedaron cargadas.
                escala.ordenadas().stream()
                        .map(b -> new BandaResponse(b.desde(), b.hasta(), b.resultado(), b.rango()))
                        .toList());
    }
}
