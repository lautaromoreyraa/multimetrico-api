package com.lautaro.multimetrico.dto.medicion;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record HistoriaResponse(
        YearMonth periodo,
        String legajo,
        Metrica metrica,
        int total,
        List<MedicionResponse> mediciones) {

    public record MedicionResponse(
            String legajo,
            Metrica metrica,
            BigDecimal valor,
            LocalDate fechaDeCorte,
            String origenImportacion,
            boolean vigente) {
    }
}
