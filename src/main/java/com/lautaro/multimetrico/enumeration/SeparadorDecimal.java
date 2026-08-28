package com.lautaro.multimetrico.enumeration;

/**
 * Qué símbolo separa la parte decimal en las celdas de texto de un archivo.
 *
 * <p>Solo entra en juego cuando el número llegó escrito como texto: una celda
 * numérica ya viene en binario y ahí no hay coma ni punto que interpretar. El
 * símbolo que no es el separador se trata como separador de miles y se descarta.
 */
public enum SeparadorDecimal {

    /** {@code 80,5} — lo normal en una planilla en español. */
    COMA,

    /** {@code 80.5} — lo normal en una exportación de un sistema en inglés. */
    PUNTO
}
