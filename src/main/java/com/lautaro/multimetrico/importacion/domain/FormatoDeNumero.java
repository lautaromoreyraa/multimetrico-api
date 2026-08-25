package com.lautaro.multimetrico.importacion.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Cómo leer el número de una celda y llevarlo a las unidades del pliego.
 *
 * <p>{@code 80,5%}, {@code 0.805} y {@code 80.5} son el mismo desempeño escrito
 * de tres maneras, y el pliego evalúa contra una sola: la banda de FCR va de
 * {@code 79.5} a {@code 80.1}, o sea <b>puntos porcentuales</b>. Elegir mal
 * entre esas tres formas no rompe nada de manera visible — produce un número
 * cien veces más grande o más chico, que cae en otra banda y sale por la API
 * como si fuera cierto. Por eso la unidad es parte del perfil y no una
 * suposición del código.
 *
 * <h2>Dos ejes independientes</h2>
 *
 * <p><b>El separador decimal es un problema del texto, no del número.</b> Una
 * celda numérica ya llega como número: Excel la guardó en binario y ahí no hay
 * coma ni punto que interpretar. El separador solo entra en juego cuando el
 * "número" llegó escrito como texto, que es justamente el defecto que el
 * proyecto vino a atacar —columnas que mezclan texto y número y hacen que las
 * sumas descarten filas en silencio—. Las planillas en español escriben
 * {@code 80,5}; las exportaciones de sistemas en inglés, {@code 80.5}. El otro
 * símbolo se trata como separador de miles y se descarta.
 *
 * <p><b>La escala dice en qué unidad viene el número</b> cuando la celda no lo
 * aclara.
 *
 * <h2>Cuándo la celda manda sobre el perfil</h2>
 *
 * <p>Hay dos casos donde el archivo <em>declara</em> su unidad, y entonces la
 * declaración gana. No es adivinar: es leer un dato que está escrito.
 *
 * <ul>
 *   <li><b>Celda numérica con formato de porcentaje.</b> Excel muestra
 *       {@code 80,5%} pero guarda {@code 0.805}. Siempre. Una celda así está en
 *       fracción por definición del formato, y se multiplica por cien.</li>
 *   <li><b>Texto que contiene {@code %}.</b> Ahí el {@code %} es un carácter más
 *       de una cadena, y el número escrito al lado —{@code 80,5}— ya está en
 *       puntos porcentuales.</li>
 * </ul>
 *
 * <p>Las dos cosas se ven igual en la pantalla y guardan números distintos, que
 * es exactamente por qué esto no se puede resolver mirando lo que muestra Excel.
 * Y es lo que permite que una columna mezclada —algunas celdas numéricas, otras
 * escritas a mano— se lea bien fila por fila, en vez de tener que elegir una
 * sola interpretación para toda la columna.
 */
public record FormatoDeNumero(SeparadorDecimal separadorDecimal, Escala escala) {

    /** Qué símbolo separa la parte decimal en las celdas de texto. */
    public enum SeparadorDecimal {

        /** {@code 80,5} — lo normal en una planilla en español. */
        COMA,

        /** {@code 80.5} — lo normal en una exportación de un sistema en inglés. */
        PUNTO
    }

    /** En qué unidad viene el número cuando la celda no lo declara. */
    public enum Escala {

        /** El número ya está en las unidades del pliego: 80,5 es 80,5. */
        TAL_CUAL,

        /** El número es una fracción de uno: 0,805 son 80,5 puntos. Se multiplica por cien. */
        FRACCION_DE_UNO
    }

    private static final BigDecimal CIEN = new BigDecimal("100");

    /**
     * Lo que sobra alrededor de un número escrito a mano: el signo de
     * porcentaje, el de moneda y cualquier espacio. El espacio duro
     * ({@code U+00A0}) va aparte porque {@code \s} no lo cubre y es el que Excel
     * usa como separador de miles en los formatos de moneda.
     */
    private static final String RUIDO = "[\\s\\u00A0%$]";

    private static final String UN_NUMERO = "[+-]?\\d+(\\.\\d+)?";

    public FormatoDeNumero {
        if (separadorDecimal == null) {
            throw new IllegalArgumentException("El formato de número necesita un separador decimal");
        }
        if (escala == null) {
            throw new IllegalArgumentException("El formato de número necesita una escala");
        }
    }

    /** Coma decimal y sin escalar: la planilla en español típica. */
    public static FormatoDeNumero estandar() {
        return new FormatoDeNumero(SeparadorDecimal.COMA, Escala.TAL_CUAL);
    }

    /**
     * El valor de la celda en las unidades del pliego, o vacío si eso no es un
     * número.
     *
     * <p>Vacío no significa cero. Significa que la fila no se puede importar y
     * hay que rechazarla diciendo qué decía la celda: es la columna donde la
     * planilla original mezcla texto con números y descarta filas sin avisar.
     */
    public Optional<BigDecimal> interpretar(ValorCrudo crudo) {
        if (crudo == null || crudo.estaVacio()) {
            return Optional.empty();
        }
        if (crudo.numero() != null) {
            // Formato de porcentaje: Excel guardó una fracción. Lo que declara el
            // archivo gana sobre la escala que supone el perfil.
            return Optional.of(crudo.formatoPorcentaje()
                    ? limpiar(crudo.numero().multiply(CIEN))
                    : escalar(crudo.numero()));
        }
        boolean signoEscrito = crudo.texto().contains("%");
        return parsear(crudo.texto())
                // El signo escrito al lado del número ya dice que está en puntos
                // porcentuales: no hay nada que escalar.
                .map(numero -> signoEscrito ? numero : escalar(numero));
    }

    private BigDecimal escalar(BigDecimal numero) {
        return escala == Escala.FRACCION_DE_UNO ? limpiar(numero.multiply(CIEN)) : numero;
    }

    /**
     * Multiplicar por cien deja ceros de más ({@code 0.805 -> 80.500}). Se sacan
     * para que el valor guardado se lea igual al que muestra la planilla, y se
     * vuelve a fijar la escala en cero cuando queda negativa, que es como
     * {@link BigDecimal#stripTrailingZeros()} representa a los redondos y lo que
     * los saca por la API en notación científica.
     */
    private static BigDecimal limpiar(BigDecimal numero) {
        BigDecimal sinCeros = numero.stripTrailingZeros();
        return sinCeros.scale() < 0 ? sinCeros.setScale(0) : sinCeros;
    }

    private Optional<BigDecimal> parsear(String texto) {
        String limpio = texto.replaceAll(RUIDO, "");

        limpio = separadorDecimal == SeparadorDecimal.COMA
                ? limpio.replace(".", "").replace(',', '.')
                : limpio.replace(",", "");

        // Un "S/D", un guion o una celda con una nota adentro no son números. Se
        // reconoce lo que es un número entero y se rechaza todo lo demás, en vez
        // de rescatar los dígitos que hayan quedado sueltos en medio de un texto:
        // ese rescate es el que convierte "no llegó el dato" en un número.
        if (!limpio.matches(UN_NUMERO)) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(limpio));
    }
}
