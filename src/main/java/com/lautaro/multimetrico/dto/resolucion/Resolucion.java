package com.lautaro.multimetrico.dto.resolucion;

import java.util.List;

/**
 * El resultado de cruzar los nombres de un archivo contra el padrón.
 *
 * <p>Lo resuelto y lo pendiente viajan juntos y separados. Es la diferencia con
 * la planilla: ahí lo que no empalma se pierde entre las filas y el total sale
 * igual, más chico, sin que nada avise. Acá una importación con pendientes es un
 * hecho visible que alguien tiene que atender.
 *
 * <p>{@code completa} viene calculada. Las agregaciones que necesitan quienes la
 * consumen —el mapa por texto original, el conteo por estrategia— las arma el
 * servicio que las usa, no este record.
 */
public record Resolucion(List<Coincidencia> resueltos, List<Pendiente> pendientes, boolean completa) {

    public Resolucion {
        resueltos = List.copyOf(resueltos);
        pendientes = List.copyOf(pendientes);
    }

    public static Resolucion de(List<Coincidencia> resueltos, List<Pendiente> pendientes) {
        return new Resolucion(resueltos, pendientes, pendientes.isEmpty());
    }
}
