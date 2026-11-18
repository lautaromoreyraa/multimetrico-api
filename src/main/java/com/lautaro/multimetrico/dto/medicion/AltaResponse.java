package com.lautaro.multimetrico.dto.medicion;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public record AltaResponse(
        YearMonth periodo,
        int registradas,
        int asesores,
        Map<Metrica, Long> porMetrica,
        List<LocalDate> cortes) {
}
