package com.lautaro.multimetrico.dto.resolucion;

import com.lautaro.multimetrico.dto.padron.Candidato;
import com.lautaro.multimetrico.enumeration.Estrategia;
import java.util.List;
import java.util.Map;

/**
 * El informe de resolución.
 *
 * <p>El resumen va primero a propósito: lo que quien sube el archivo necesita ver
 * de un vistazo es cuántas filas no empalmaron. Que sean cero es lo normal; que
 * no lo sean tiene que doler a la vista.
 *
 * <p>{@code porEstrategia} no es una curiosidad estadística: muchas resoluciones
 * por {@code PREFIJO_UNICO} significan que el archivo viene truncando nombres, y
 * eso es algo para mirar antes de que un truncado se vuelva ambiguo.
 */
public record ResolucionResponse(
        int total,
        int resueltos,
        int pendientes,
        boolean completa,
        Map<Estrategia, Long> porEstrategia,
        List<CoincidenciaResponse> coincidencias,
        List<PendienteResponse> sinResolver) {

    public record CoincidenciaResponse(String texto, String legajo, String nombre, Estrategia via) {
    }

    /** El {@code motivo} va redactado, no como enum: es lo que se muestra. */
    public record PendienteResponse(String texto, String motivo, List<Candidato> candidatos) {
    }
}
