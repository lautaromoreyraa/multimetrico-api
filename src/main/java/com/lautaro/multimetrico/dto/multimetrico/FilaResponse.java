package com.lautaro.multimetrico.dto.multimetrico;

import com.lautaro.multimetrico.enumeration.CondicionDeTramo;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.Sentido;
import com.lautaro.multimetrico.enumeration.Unidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FilaResponse(
        String legajo,
        String nombre,
        String lider,
        BigDecimal alcanceTotal,
        boolean completa,
        LocalDate actualizadoDesde,
        List<CeldaResponse> metricas,
        List<Metrica> faltantes,
        List<Metrica> fueraDeEscala) {

    public record CeldaResponse(
            Metrica metrica,
            String descripcion,
            Sentido sentido,
            Unidad unidad,
            BigDecimal valor,
            String banda,
            BigDecimal resultado,
            LocalDate actualizadoAl,
            TramoResponse tramoDeArriba) {
    }

    public record TramoResponse(
            String banda,
            BigDecimal objetivo,
            BigDecimal distancia,
            CondicionDeTramo condicion,
            BigDecimal resultado,
            BigDecimal mejora,
            String detalle) {
    }
}
