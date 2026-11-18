package com.lautaro.multimetrico.util;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public final class PeriodoUtil {

    public static final String PATRON = "yyyy-MM";

    private PeriodoUtil() {
    }

    public static String aTexto(YearMonth periodo) {
        return periodo == null ? null : periodo.toString();
    }

    public static YearMonth deTexto(String texto) {
        return texto == null || texto.isBlank() ? null : YearMonth.parse(texto);
    }

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
