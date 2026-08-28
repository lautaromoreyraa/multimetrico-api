package com.lautaro.multimetrico.dto.resolucion;

import com.lautaro.multimetrico.dto.padron.Candidato;
import com.lautaro.multimetrico.enumeration.MotivoDePendiente;
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
