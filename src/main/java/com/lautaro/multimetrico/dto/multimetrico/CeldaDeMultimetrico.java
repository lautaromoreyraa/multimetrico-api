package com.lautaro.multimetrico.dto.multimetrico;

import com.lautaro.multimetrico.dto.evaluacion.Alcance;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.time.LocalDate;
import java.util.Optional;

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

    public Optional<TramoDeArriba> tramoDeArriba() {
        return Optional.ofNullable(tramo);
    }
}
