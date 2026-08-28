package com.lautaro.multimetrico.dto.medicion;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

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
}
