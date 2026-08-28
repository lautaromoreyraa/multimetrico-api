package com.lautaro.multimetrico.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lo que se puede ajustar del procesamiento de importaciones sin recompilar.
 *
 * <p>Los dos números tienen consecuencias distintas y por eso están separados:
 *
 * <ul>
 *   <li>{@code hilos} es cuántas importaciones se procesan a la vez. Leer una
 *       planilla entera con POI la carga en memoria, así que subirlo es la forma
 *       más rápida de quedarse sin heap con archivos grandes.</li>
 *   <li>{@code cola} es cuántas esperan. <b>Tiene tope a propósito</b>: sin tope
 *       una avalancha de cargas se acumula en silencio hasta reventar; con tope,
 *       la que sobra falla de entrada y se ve.</li>
 * </ul>
 *
 * <p>Los valores por defecto son los de una tarea semanal de un equipo, que es el
 * uso real. Están acá y no en {@code application.yml} para que el archivo de
 * configuración pueda no mencionarlos.
 *
 * @param hilos importaciones procesándose al mismo tiempo
 * @param cola importaciones esperando turno antes de que la siguiente sea rechazada
 */
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
