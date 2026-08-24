package com.lautaro.multimetrico.multimetrico.domain;

import com.lautaro.multimetrico.pliego.domain.Alcance;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Una métrica de un asesor en el multimétrico: el valor, la banda en que cayó,
 * lo que paga, <b>a qué fecha está</b> y qué le falta para el tramo de arriba.
 *
 * <p>La razón de que esto sea un objeto y no cuatro listas paralelas dentro de
 * la fila es el {@code actualizadoAl}. En la planilla el número y su fecha de
 * corte viven en hojas distintas —el valor en la del multimétrico, la fecha en
 * el encabezado del archivo del que se copió y pegó— y al juntarse se pierde:
 * el multimétrico muestra un TMO al viernes y un FCR al lunes anterior como si
 * fueran la misma foto. Acá la fecha está en el mismo objeto que el valor, así
 * que no hay forma de mostrar uno sin el otro salvo decidiendo omitirla.
 *
 * <p>De dónde sale cada cosa: {@code alcance} lo calcula el pliego —es él quien
 * sabe en qué banda cae un valor y cuánto paga—, {@code actualizadoAl} es la
 * fecha de corte de la medición que se usó, y {@code tramo} sale de la escala.
 */
public record CeldaDeMultimetrico(Alcance alcance, LocalDate actualizadoAl, TramoDeArriba tramo) {

    public CeldaDeMultimetrico {
        if (alcance == null) {
            throw new IllegalArgumentException("La celda necesita un alcance");
        }
        if (actualizadoAl == null) {
            throw new IllegalArgumentException(
                    "La celda de %s necesita la fecha de corte de la medición que se usó"
                            .formatted(alcance.metrica()));
        }
    }

    public Metrica metrica() {
        return alcance.metrica();
    }

    /** Vacío cuando el asesor ya está en la mejor banda de la escala. */
    public Optional<TramoDeArriba> tramoDeArriba() {
        return Optional.ofNullable(tramo);
    }
}
