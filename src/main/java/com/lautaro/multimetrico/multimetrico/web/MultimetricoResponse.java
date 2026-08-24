package com.lautaro.multimetrico.multimetrico.web;

import com.lautaro.multimetrico.multimetrico.domain.Multimetrico;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * El multimétrico de un equipo tal como sale por HTTP.
 *
 * <p>El resumen va arriba de las filas por la misma razón que en el informe de
 * resolución del padrón: lo primero que hay que ver es cuántas filas están
 * incompletas y desde qué fecha se puede confiar en el total. Si eso queda al
 * final de doscientas filas, no lo mira nadie.
 */
public record MultimetricoResponse(
        YearMonth periodo,
        String campania,
        String lider,
        BigDecimal alcanceMaximo,
        Resumen resumen,
        List<FilaResponse> filas) {

    /**
     * @param actualizadoDesde el corte más viejo del equipo: hasta ahí llega,
     *     de verdad, lo que muestra esta tabla
     * @param cortesPorMetrica más de una fecha en una métrica significa que
     *     alguien quedó afuera del último archivo
     */
    public record Resumen(
            int asesores,
            long completos,
            long conFaltantes,
            LocalDate actualizadoDesde,
            Map<Metrica, Set<LocalDate>> cortesPorMetrica) {
    }

    public static MultimetricoResponse de(Multimetrico multimetrico) {
        Resumen resumen = new Resumen(
                multimetrico.asesores(),
                multimetrico.completos(),
                multimetrico.conFaltantes(),
                multimetrico.actualizadoDesde().orElse(null),
                multimetrico.cortesDelEquipo());

        return new MultimetricoResponse(
                multimetrico.periodo(),
                multimetrico.campania(),
                multimetrico.lider(),
                multimetrico.alcanceMaximo(),
                resumen,
                multimetrico.filas().stream().map(FilaResponse::de).toList());
    }
}
