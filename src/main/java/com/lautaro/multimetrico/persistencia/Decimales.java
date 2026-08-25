package com.lautaro.multimetrico.persistencia;

import java.math.BigDecimal;

/**
 * Devuelve un {@link BigDecimal} leído de la base a la escala con la que entró.
 *
 * <p>Las columnas son {@code DECIMAL(18,6)} porque sin precisión y escala
 * explícitas MySQL redondea a entero, y todo el cálculo de este proyecto compara
 * contra bordes de banda. El precio es que la base devuelve siempre seis
 * decimales: un 80,1 cargado sale como {@code 80.100000}.
 *
 * <p>Para el motor de bandas eso da igual —el dominio compara con
 * {@code compareTo}, nunca con {@code equals}—, pero <b>no da igual para lo que
 * se muestra</b>. Los mensajes que lee una persona se arman con
 * {@code toPlainString()}: el rango de una banda ({@code "[-∞, 80.1)"}) y el
 * detalle del tramo de arriba ({@code "sobran 12"}). Sin este paso quedarían
 * {@code "[-∞, 80.100000)"} y {@code "sobran 12.000000"}.
 *
 * <p>El {@code setScale(0)} de abajo no es cosmético: {@code stripTrailingZeros}
 * convierte {@code 600.000000} en {@code 6E+2}, que tiene escala negativa y se
 * imprime como {@code "6E+2"}. Volverlo a escala 0 lo deja en {@code "600"}.
 */
public final class Decimales {

    private Decimales() {
    }

    public static BigDecimal comoEntro(BigDecimal deLaBase) {
        if (deLaBase == null) {
            return null;
        }
        BigDecimal sinCeros = deLaBase.stripTrailingZeros();
        return sinCeros.scale() < 0 ? sinCeros.setScale(0) : sinCeros;
    }
}
