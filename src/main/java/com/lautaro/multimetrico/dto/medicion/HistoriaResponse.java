package com.lautaro.multimetrico.dto.medicion;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * La historia de un mes, con cada medición marcada como vigente o superada.
 *
 * <p>La marca es el punto de este endpoint. Sin ella hay una lista de números
 * con fechas y el que la lee tiene que deducir cuál es el que se está usando;
 * con ella se ve de una: "tu FCR es 78,2 porque es la carga del 22, la del 15
 * decía 74,1". Es la conversación que hoy no se puede tener.
 *
 * <p>La vigencia se calcula <b>por asesor y por métrica</b>, no sobre la lista
 * entera: pedir la historia de un mes sin filtrar trae a toda la operación, y
 * ahí hay una vigente por cada persona. Quien la calcula es
 * {@code MedicionService}; acá solo llega marcada.
 *
 * <p>Las filas salen <b>en orden de carga</b>, no ordenadas por fecha de corte:
 * es el orden en el que se decide cuál vale, y verlo tal cual es lo que permite
 * reconstruir qué pasó.
 */
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
