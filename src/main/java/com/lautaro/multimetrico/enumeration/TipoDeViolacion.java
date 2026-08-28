package com.lautaro.multimetrico.enumeration;

/** Qué clase de problema tiene un pliego que no se puede usar. */
public enum TipoDeViolacion {
    SUPERPOSICION,
    HUECO,
    SIN_EXTREMO_ABIERTO,
    PESO_NO_COINCIDE,
    METRICA_DUPLICADA,
    BANDAS_INSUFICIENTES
}
