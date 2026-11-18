package com.lautaro.multimetrico.dto.resolucion;

import com.lautaro.multimetrico.dto.padron.Candidato;
import com.lautaro.multimetrico.enumeration.Estrategia;
import java.util.List;
import java.util.Map;

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

    public record PendienteResponse(String texto, String motivo, List<Candidato> candidatos) {
    }
}
