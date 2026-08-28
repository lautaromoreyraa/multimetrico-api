package com.lautaro.multimetrico.enumeration;

/**
 * Por qué un nombre quedó sin resolver.
 *
 * <p>Los dos casos piden acciones distintas: al {@link #SIN_CANDIDATOS} le falta
 * un alta en el padrón, el {@link #AMBIGUO} necesita que alguien desempate.
 */
public enum MotivoDePendiente {
    SIN_CANDIDATOS,
    AMBIGUO
}
