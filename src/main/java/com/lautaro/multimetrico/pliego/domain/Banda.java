package com.lautaro.multimetrico.pliego.domain;

import java.math.BigDecimal;

/**
 * Un tramo del pliego: el rango de valores que paga un mismo alcance.
 *
 * <p><b>El intervalo es semiabierto: {@code [desde, hasta)}.</b> El límite de
 * abajo entra, el de arriba no, y el {@code hasta} de una banda es exactamente
 * el {@code desde} de la siguiente. Es la decisión que sostiene todo lo demás,
 * así que conviene decir por qué.
 *
 * <p>Los pliegos en planilla se escriben con los dos extremos incluidos y con
 * el número ya redondeado para mostrar: {@code 0–642}, después {@code 643–655};
 * {@code 79,5%–80,0%}, después {@code 80,1%–81,1%}. Eso deja dos agujeros que
 * no se ven hasta que muerden:
 *
 * <ul>
 *   <li><b>Huecos.</b> Entre el {@code 80,0%} de una banda y el {@code 80,1%}
 *       de la siguiente no hay nada. Un FCR de 80,04% se muestra como "80,0%"
 *       redondeado, pero el valor que se compara es el completo, y no cae en
 *       ninguna banda.</li>
 *   <li><b>Superposiciones.</b> Basta un dígito mal tipeado —una banda que
 *       termina en 681 y la siguiente que arranca en 680— para que un valor
 *       pertenezca a dos bandas a la vez. La planilla resuelve por la primera
 *       que encuentra y no avisa.</li>
 * </ul>
 *
 * <p>Con intervalos semiabiertos las dos cosas dejan de ser posibles por
 * construcción: si {@code hasta[n] == desde[n+1]}, no hay lugar donde meter un
 * hueco ni forma de que dos bandas se pisen. Lo que antes era un error silencioso
 * pasa a ser una condición que el validador puede exigir y nombrar.
 *
 * <p>Los extremos abiertos van en {@code null}: {@code desde == null} es "sin
 * mínimo" y {@code hasta == null} es "sin máximo". Esto no es un detalle
 * cosmético — es lo que evita el otro error clásico. Si la mejor banda de FCR
 * fuera {@code [81,9%, 100%)}, un FCR perfecto de 100% quedaría afuera de la
 * escala. Abierta hacia arriba, no.
 *
 * <p>Todo en {@link BigDecimal} y comparado con {@code compareTo}: los valores
 * caen justo sobre los bordes de banda todo el tiempo, que es exactamente donde
 * el punto flotante binario falla.
 */
public record Banda(BigDecimal desde, BigDecimal hasta, BigDecimal resultado) {

    public Banda {
        if (resultado == null) {
            throw new IllegalArgumentException("La banda necesita un resultado");
        }
        if (desde != null && hasta != null && desde.compareTo(hasta) >= 0) {
            throw new IllegalArgumentException(
                    "El desde (%s) tiene que ser menor que el hasta (%s)".formatted(desde, hasta));
        }
    }

    /** {@code desde <= valor < hasta}, con los nulos valiendo por infinito. */
    public boolean contiene(BigDecimal valor) {
        boolean pasaElPiso = desde == null || valor.compareTo(desde) >= 0;
        boolean pasaElTecho = hasta == null || valor.compareTo(hasta) < 0;
        return pasaElPiso && pasaElTecho;
    }

    public boolean abiertaAbajo() {
        return desde == null;
    }

    public boolean abiertaArriba() {
        return hasta == null;
    }

    /** Para los mensajes del validador, que es lo que va a leer una persona. */
    public String rango() {
        String piso = desde == null ? "-∞" : desde.toPlainString();
        String techo = hasta == null ? "+∞" : hasta.toPlainString();
        return "[%s, %s)".formatted(piso, techo);
    }
}
