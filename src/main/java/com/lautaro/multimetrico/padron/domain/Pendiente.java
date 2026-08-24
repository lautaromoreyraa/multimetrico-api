package com.lautaro.multimetrico.padron.domain;

import java.util.List;

/**
 * Un nombre que el sistema no supo resolver.
 *
 * <p>Es la salida más importante del padrón. Hoy una fila que no empalma
 * simplemente no llega al multimétrico, y el asesor se entera cuando reclama.
 * Acá queda listada con nombre y apellido, y con los candidatos que se le
 * parecen.
 *
 * <p>Los candidatos son <b>sugerencias para que una persona confirme</b>, nunca
 * una resolución automática. En un cálculo que afecta lo que alguien cobra, un
 * parecido no alcanza: si el sistema adivina mal, le atribuye a alguien las
 * métricas de otro y no queda rastro de que fue una suposición.
 *
 * <p>El {@code motivo} distingue el que no se parece a nadie del que se parece a
 * varios, que son problemas distintos: al primero le falta un alta en el padrón,
 * el segundo necesita que alguien desempate.
 */
public record Pendiente(String textoOriginal, Motivo motivo, List<Asesor> candidatos) {

    public enum Motivo {
        SIN_CANDIDATOS,
        AMBIGUO
    }

    public Pendiente {
        candidatos = List.copyOf(candidatos);
    }

    public static Pendiente sinCandidatos(String texto, List<Asesor> sugerencias) {
        return new Pendiente(texto, Motivo.SIN_CANDIDATOS, sugerencias);
    }

    public static Pendiente ambiguo(String texto, List<Asesor> candidatos) {
        return new Pendiente(texto, Motivo.AMBIGUO, candidatos);
    }
}
