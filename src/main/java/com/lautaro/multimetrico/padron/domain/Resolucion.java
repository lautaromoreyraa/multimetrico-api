package com.lautaro.multimetrico.padron.domain;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El resultado de cruzar los nombres de un archivo contra el padrón.
 *
 * <p>Lo resuelto y lo pendiente viajan juntos y separados. Es la diferencia con
 * la planilla: ahí lo que no empalma se pierde entre las filas y el total sale
 * igual, más chico, sin que nada avise. Acá una importación con pendientes es un
 * hecho visible que alguien tiene que atender.
 */
public record Resolucion(List<Coincidencia> resueltos, List<Pendiente> pendientes) {

    public Resolucion {
        resueltos = List.copyOf(resueltos);
        pendientes = List.copyOf(pendientes);
    }

    public boolean completa() {
        return pendientes.isEmpty();
    }

    public Map<String, Asesor> porTextoOriginal() {
        return resueltos.stream().collect(Collectors.toMap(
                Coincidencia::textoOriginal, Coincidencia::asesor, (a, b) -> a));
    }

    /** Cuántas se resolvieron por cada vía; sirve para saber cómo viene el archivo. */
    public Map<Estrategia, Long> porEstrategia() {
        return resueltos.stream().collect(Collectors.groupingBy(
                Coincidencia::estrategia, Collectors.counting()));
    }

    public Map<Pendiente.Motivo, List<Pendiente>> pendientesPorMotivo() {
        return pendientes.stream().collect(Collectors.groupingBy(Pendiente::motivo,
                Collectors.collectingAndThen(Collectors.toList(), Function.identity())));
    }
}
