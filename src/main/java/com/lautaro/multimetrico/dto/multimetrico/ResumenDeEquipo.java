package com.lautaro.multimetrico.dto.multimetrico;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * Lo que hay que ver antes de mirar las filas.
 *
 * <p>Va arriba de la tabla por la misma razón que en el informe de resolución del
 * padrón: lo primero que hay que saber es cuántas filas están incompletas y desde
 * qué fecha se puede confiar en el total. Si eso queda al final de doscientas
 * filas, no lo mira nadie.
 *
 * @param actualizadoDesde el corte más viejo del equipo: hasta ahí llega, de
 *     verdad, lo que muestra esta tabla
 * @param cortesPorMetrica las fechas que aparecen en cada métrica. Lo normal es
 *     una sola: el archivo semanal trae a todo el mundo con el mismo corte.
 *     <b>Que haya dos es una señal</b> —quedó gente afuera del último archivo y
 *     sigue mostrando el número de la semana pasada—, y es exactamente la clase
 *     de cosa que hoy no se ve hasta que alguien reclama.
 */
public record ResumenDeEquipo(
        int asesores,
        long completos,
        long conFaltantes,
        LocalDate actualizadoDesde,
        Map<Metrica, Set<LocalDate>> cortesPorMetrica) {
}
