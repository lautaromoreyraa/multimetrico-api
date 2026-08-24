package com.lautaro.multimetrico.medicion.web;

import com.lautaro.multimetrico.medicion.domain.Medicion;
import com.lautaro.multimetrico.medicion.domain.MedicionesVigentes;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * ahí hay una vigente por cada persona.
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

    public static HistoriaResponse de(YearMonth periodo, String legajo, Metrica metrica, List<Medicion> historia) {
        Map<String, List<Medicion>> porLegajo = new LinkedHashMap<>();
        for (Medicion medicion : historia) {
            porLegajo.computeIfAbsent(medicion.legajo().toUpperCase(java.util.Locale.ROOT),
                    clave -> new ArrayList<>()).add(medicion);
        }

        // Cada asesor tiene su propia vigente por métrica; se resuelven de a uno
        // y después se vuelve al orden de carga original, que es el que cuenta
        // la historia.
        List<Medicion> vigentes = new ArrayList<>();
        porLegajo.values().forEach(deUno -> vigentes.addAll(MedicionesVigentes.de(deUno).porMetrica().values()));

        List<MedicionResponse> filas = historia.stream()
                .map(medicion -> new MedicionResponse(
                        medicion.legajo(),
                        medicion.metrica(),
                        medicion.valor(),
                        medicion.fechaDeCorte(),
                        medicion.origenImportacion(),
                        // Por valor y no por identidad, para que siga andando cuando
                        // detrás haya una base y cada consulta traiga instancias nuevas.
                        vigentes.contains(medicion)))
                .toList();

        return new HistoriaResponse(periodo, legajo, metrica, filas.size(), filas);
    }
}
