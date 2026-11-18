package com.lautaro.multimetrico.dto.resolucion;

import com.lautaro.multimetrico.dto.padron.Candidato;
import com.lautaro.multimetrico.enumeration.MotivoDePendiente;
import java.util.List;

public record Pendiente(String textoOriginal, MotivoDePendiente motivo, List<Candidato> candidatos) {

    public Pendiente {
        candidatos = List.copyOf(candidatos == null ? List.of() : candidatos);
    }

    public static Pendiente sinCandidatos(String texto, List<Candidato> sugerencias) {
        return new Pendiente(texto, MotivoDePendiente.SIN_CANDIDATOS, sugerencias);
    }

    public static Pendiente ambiguo(String texto, List<Candidato> candidatos) {
        return new Pendiente(texto, MotivoDePendiente.AMBIGUO, candidatos);
    }
}
