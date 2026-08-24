package com.lautaro.multimetrico.padron.web;

import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.padron.domain.Coincidencia;
import com.lautaro.multimetrico.padron.domain.Estrategia;
import com.lautaro.multimetrico.padron.domain.Pendiente;
import com.lautaro.multimetrico.padron.domain.Resolucion;
import java.util.List;
import java.util.Map;

/**
 * El informe de resolución.
 *
 * <p>El resumen va primero a propósito: lo que quien sube el archivo necesita ver
 * de un vistazo es cuántas filas no empalmaron. Que sean cero es lo normal; que
 * no lo sean tiene que doler a la vista.
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

    public record PendienteResponse(String texto, String motivo, List<CandidatoResponse> candidatos) {
    }

    public record CandidatoResponse(String legajo, String nombre) {
    }

    public static ResolucionResponse de(Resolucion resolucion) {
        return new ResolucionResponse(
                resolucion.resueltos().size() + resolucion.pendientes().size(),
                resolucion.resueltos().size(),
                resolucion.pendientes().size(),
                resolucion.completa(),
                resolucion.porEstrategia(),
                resolucion.resueltos().stream().map(ResolucionResponse::coincidencia).toList(),
                resolucion.pendientes().stream().map(ResolucionResponse::pendiente).toList());
    }

    private static CoincidenciaResponse coincidencia(Coincidencia c) {
        return new CoincidenciaResponse(c.textoOriginal(), c.asesor().legajo(), c.asesor().nombre(), c.estrategia());
    }

    private static PendienteResponse pendiente(Pendiente p) {
        return new PendienteResponse(p.textoOriginal(), motivo(p),
                p.candidatos().stream().map(ResolucionResponse::candidato).toList());
    }

    private static CandidatoResponse candidato(Asesor asesor) {
        return new CandidatoResponse(asesor.legajo(), asesor.nombre());
    }

    private static String motivo(Pendiente pendiente) {
        return switch (pendiente.motivo()) {
            case SIN_CANDIDATOS -> pendiente.candidatos().isEmpty()
                    ? "no se parece a nadie del padrón"
                    : "no coincide con nadie; estos se le parecen";
            case AMBIGUO -> "el nombre viene cortado y coincide con más de un asesor";
        };
    }
}
