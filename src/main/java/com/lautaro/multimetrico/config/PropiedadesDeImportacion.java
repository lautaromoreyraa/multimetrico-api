package com.lautaro.multimetrico.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "multimetrico.importacion")
public record PropiedadesDeImportacion(Integer hilos, Integer cola) {

    private static final int HILOS_POR_DEFECTO = 2;
    private static final int COLA_POR_DEFECTO = 50;

    public PropiedadesDeImportacion {
        hilos = hilos == null ? HILOS_POR_DEFECTO : hilos;
        cola = cola == null ? COLA_POR_DEFECTO : cola;

        if (hilos < 1) {
            throw new IllegalArgumentException(
                    "multimetrico.importacion.hilos tiene que ser al menos 1; llegó %d".formatted(hilos));
        }
        if (cola < 1) {
            throw new IllegalArgumentException(
                    "multimetrico.importacion.cola tiene que ser al menos 1; llegó %d".formatted(cola));
        }
    }
}
