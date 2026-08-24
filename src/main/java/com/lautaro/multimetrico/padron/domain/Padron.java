package com.lautaro.multimetrico.padron.domain;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Quién estaba en la operación en un período, y bajo qué líder.
 *
 * <p>Va por período como el pliego, y por la misma razón: la gente entra y sale,
 * y el multimétrico de marzo tiene que poder rearmarse con la gente que había en
 * marzo. Si el padrón fuera uno solo y siempre el actual, un asesor que renunció
 * desaparecería de sus propios meses.
 *
 * <p><b>Pero a diferencia del pliego, el padrón sí se puede reemplazar.</b> No es
 * un descuido de consistencia: son cosas distintas. El pliego es el compromiso
 * del mes y pisarlo cambia resultados ya calculados; el padrón es una lista de
 * personas que cambia dentro del mismo mes cada vez que alguien ingresa. Volver
 * a cargarlo es la operación normal, no una corrección excepcional.
 *
 * <p>Guarda <b>todos</b> los equipos, no solo uno. Es la forma del archivo de
 * origen —la nómina viene con la operación entera y una columna de líder— y es lo
 * que permite que el sistema haga el filtrado que hoy hace cada supervisor a mano.
 */
public record Padron(YearMonth periodo, List<Asesor> asesores) {

    public Padron {
        if (periodo == null) {
            throw new IllegalArgumentException("El padrón necesita un período");
        }
        if (asesores == null || asesores.isEmpty()) {
            throw new IllegalArgumentException("El padrón no tiene asesores");
        }
        asesores = List.copyOf(asesores);

        Map<String, Asesor> porLegajo = new LinkedHashMap<>();
        for (Asesor asesor : asesores) {
            Asesor previo = porLegajo.putIfAbsent(asesor.legajo(), asesor);
            if (previo != null) {
                throw new IllegalArgumentException(
                        "El legajo %s aparece dos veces en el padrón: '%s' y '%s'"
                                .formatted(asesor.legajo(), previo.nombre(), asesor.nombre()));
            }
        }
    }

    public Optional<Asesor> porLegajo(String legajo) {
        return asesores.stream()
                .filter(asesor -> asesor.legajo().equalsIgnoreCase(legajo.trim()))
                .findFirst();
    }

    /**
     * El equipo de un líder.
     *
     * <p>Esto es el paso que hoy se hace a mano: abrir el archivo con todos los
     * equipos y filtrar por el propio nombre, en cada métrica, todas las semanas.
     */
    public List<Asesor> equipoDe(String lider) {
        NombreNormalizado buscado = NombreNormalizado.de(lider);
        return asesores.stream()
                .filter(asesor -> asesor.liderNormalizado().equals(buscado))
                .sorted(Comparator.comparing(Asesor::nombre))
                .toList();
    }

    public List<String> lideres() {
        return asesores.stream()
                .map(Asesor::lider)
                .distinct()
                .sorted()
                .toList();
    }

    public int tamano() {
        return asesores.size();
    }
}
