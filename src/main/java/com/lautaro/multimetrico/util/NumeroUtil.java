package com.lautaro.multimetrico.util;

import com.lautaro.multimetrico.domain.FormatoDeNumero;
import com.lautaro.multimetrico.dto.planilla.ValorCrudo;
import com.lautaro.multimetrico.enumeration.EscalaDeNumero;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Lee el número de una celda y lo lleva a las unidades del pliego.
 *
 * <p>{@code 80,5%}, {@code 0.805} y {@code 80.5} son el mismo desempeño escrito
 * de tres maneras, y el pliego evalúa contra una sola: la banda de FCR va de
 * {@code 79.5} a {@code 80.1}, o sea <b>puntos porcentuales</b>. Elegir mal
 * entre esas tres formas no rompe nada de manera visible — produce un número
 * cien veces más grande o más chico, que cae en otra banda y sale por la API
 * como si fuera cierto.
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
 *
 * <p>Vive acá y no en {@link FormatoDeNumero} porque ese es un objeto embebido de
 * persistencia: no tiene por qué conocer el {@code ValorCrudo} que sale del lector
 * de planillas.
 */
public final class NumeroUtil {

    private static final BigDecimal CIEN = new BigDecimal("100");

    /**
     * Lo que sobra alrededor de un número escrito a mano: el signo de
     * porcentaje, el de moneda y cualquier espacio. El espacio duro
     * ({@code U+00A0}) va aparte porque {@code \s} no lo cubre y es el que Excel
     * usa como separador de miles en los formatos de moneda.
     */
    private static final String RUIDO = "[\\s\\u00A0%$]";

    private static final String UN_NUMERO = "[+-]?\\d+(\\.\\d+)?";

    private NumeroUtil() {
    }

    /**
     * El valor de la celda en las unidades del pliego, o vacío si eso no es un
     * número.
     *
     * <p>Vacío no significa cero. Significa que la fila no se puede importar y
     * hay que rechazarla diciendo qué decía la celda: es la columna donde la
     * planilla original mezcla texto con números y descarta filas sin avisar.
     */
    public static Optional<BigDecimal> interpretar(FormatoDeNumero formato, ValorCrudo crudo) {
        if (crudo == null || crudo.estaVacio()) {
            return Optional.empty();
        }
        if (crudo.numero() != null) {
            // Formato de porcentaje: Excel guardó una fracción. Lo que declara el
            // archivo gana sobre la escala que supone el perfil.
            return Optional.of(crudo.formatoPorcentaje()
                    ? limpiar(crudo.numero().multiply(CIEN))
                    : escalar(formato, crudo.numero()));
        }
        boolean signoEscrito = crudo.texto().contains("%");
        return parsear(formato, crudo.texto())
                // El signo escrito al lado del número ya dice que está en puntos
                // porcentuales: no hay nada que escalar.
                .map(numero -> signoEscrito ? numero : escalar(formato, numero));
    }

    private static BigDecimal escalar(FormatoDeNumero formato, BigDecimal numero) {
        return formato.escala() == EscalaDeNumero.FRACCION_DE_UNO
                ? limpiar(numero.multiply(CIEN))
                : numero;
    }

    /**
     * Multiplicar por cien deja ceros de más ({@code 0.805 -> 80.500}). Se sacan
     * para que el valor guardado se lea igual al que muestra la planilla, y se
     * vuelve a fijar la escala en cero cuando queda negativa, que es como
     * {@link BigDecimal#stripTrailingZeros()} representa a los redondos y lo que
     * los saca por la API en notación científica.
     *
     * <p>Es la misma operación que {@link DecimalUtil#comoEntro}, que resuelve lo
     * mismo del lado de la base.
     */
    private static BigDecimal limpiar(BigDecimal numero) {
        return DecimalUtil.comoEntro(numero);
    }

    private static Optional<BigDecimal> parsear(FormatoDeNumero formato, String texto) {
        String limpio = texto.replaceAll(RUIDO, "");

        limpio = formato.separadorDecimal() == SeparadorDecimal.COMA
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
