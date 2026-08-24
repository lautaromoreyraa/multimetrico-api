package com.lautaro.multimetrico.medicion.web;

import com.lautaro.multimetrico.medicion.domain.Medicion;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * El acuse de un alta en lote.
 *
 * <p>Devuelve el desglose por métrica y los cortes que entraron, no solo un
 * "201 ok". Quien sube un archivo de doscientas filas tiene que poder ver en el
 * acto si entraron doscientas o ciento noventa, y con qué fecha quedaron: el
 * defecto que este proyecto ataca es justamente el de las filas que se pierden
 * sin que nadie se entere.
 *
 * <p>Más de un corte en la lista no es un error —el lote admite ítems con corte
 * propio—, pero es algo que conviene ver: si un archivo de una sola semana
 * devuelve dos fechas, alguna fila trae la fecha equivocada.
 */
public record AltaResponse(
        YearMonth periodo,
        int registradas,
        int asesores,
        Map<Metrica, Long> porMetrica,
        List<LocalDate> cortes) {

    public static AltaResponse de(YearMonth periodo, List<Medicion> registradas) {
        Map<Metrica, Long> porMetrica = registradas.stream()
                .collect(Collectors.groupingBy(Medicion::metrica,
                        () -> new EnumMap<>(Metrica.class), Collectors.counting()));

        return new AltaResponse(
                periodo,
                registradas.size(),
                (int) registradas.stream().map(medicion -> medicion.legajo().toUpperCase(java.util.Locale.ROOT))
                        .distinct().count(),
                porMetrica,
                registradas.stream().map(Medicion::fechaDeCorte).distinct().sorted().toList());
    }
}
