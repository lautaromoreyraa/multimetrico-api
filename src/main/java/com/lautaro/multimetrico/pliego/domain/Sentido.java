package com.lautaro.multimetrico.pliego.domain;

/**
 * Hacia dónde mejora una métrica.
 *
 * <p>No interviene en la búsqueda de la banda —para eso solo importa qué rango
 * contiene el valor—, pero sí en cómo se presenta y en qué extremo queda la
 * banda que más paga. En TMO y en bajas, menos es mejor; en NPS, FCR y
 * actividad comercial, más.
 */
public enum Sentido {
    MAYOR_ES_MEJOR,
    MENOR_ES_MEJOR
}
