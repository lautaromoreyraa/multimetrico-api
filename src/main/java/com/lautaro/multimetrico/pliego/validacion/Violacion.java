package com.lautaro.multimetrico.pliego.validacion;

import com.lautaro.multimetrico.pliego.domain.Metrica;

/**
 * Un problema concreto encontrado en un pliego.
 *
 * <p>El mensaje se escribe para que lo lea la persona que cargó el archivo, no
 * para un log: dice qué bandas, qué valores y qué consecuencia tiene. "Las
 * bandas se superponen" no sirve; "un TMO entre 680 y 681 cae en dos bandas"
 * manda a mirar la fila exacta.
 */
public record Violacion(Metrica metrica, Tipo tipo, String mensaje) {

    public enum Tipo {
        SUPERPOSICION,
        HUECO,
        SIN_EXTREMO_ABIERTO,
        PESO_NO_COINCIDE,
        METRICA_DUPLICADA,
        BANDAS_INSUFICIENTES
    }

    @Override
    public String toString() {
        return metrica == null ? mensaje : "%s: %s".formatted(metrica, mensaje);
    }
}
