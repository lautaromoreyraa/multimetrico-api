package com.lautaro.multimetrico.util;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * El período del multimétrico, siempre en la misma forma: {@code aaaa-mm}.
 *
 * <p>Un {@link YearMonth} aparece en tres bordes distintos del sistema —la URL
 * de casi todos los endpoints, la columna {@code periodo} de cinco tablas y el
 * JSON que sale por la API— y en los tres tiene que verse igual. Este es el
 * único lugar donde está escrito el formato.
 *
 * <p>{@link #PATRON} es una constante de compilación a propósito: los
 * controladores la usan dentro de {@code @DateTimeFormat}, que solo acepta
 * literales. Sin esto el patrón quedaría copiado en cada firma de cada método,
 * que es como se termina teniendo un endpoint que espera {@code aaaa-mm} y otro
 * que espera {@code mm-aaaa}.
 *
 * <p>El texto ordena bien —{@code '2026-08' < '2026-09' < '2027-01'}
 * alfabéticamente—, así que un {@code ORDER BY periodo} sale cronológico sin
 * funciones sobre la columna que maten el índice.
 */
public final class PeriodoUtil {

    /** El formato del período en la URL, en la base y en el JSON. */
    public static final String PATRON = "yyyy-MM";

    private PeriodoUtil() {
    }

    /** El texto que va a la base y sale por la API. */
    public static String aTexto(YearMonth periodo) {
        return periodo == null ? null : periodo.toString();
    }

    public static YearMonth deTexto(String texto) {
        return texto == null || texto.isBlank() ? null : YearMonth.parse(texto);
    }

    /**
     * Lo mismo que {@link #deTexto} pero con un mensaje que se puede mostrar.
     *
     * <p>El {@link DateTimeParseException} de la biblioteca dice
     * <em>"Text '2026/08' could not be parsed at index 4"</em>, que le sirve a
     * quien escribió el código y a nadie más.
     */
    public static YearMonth exigir(String texto) {
        try {
            YearMonth periodo = deTexto(texto);
            if (periodo == null) {
                throw new IllegalArgumentException("El período es obligatorio y va en formato aaaa-mm");
            }
            return periodo;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "'%s' no es un período válido; va en formato aaaa-mm, por ejemplo 2026-08".formatted(texto));
        }
    }
}
