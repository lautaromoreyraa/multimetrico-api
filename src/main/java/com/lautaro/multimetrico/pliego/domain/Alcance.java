package com.lautaro.multimetrico.pliego.domain;

import java.math.BigDecimal;

/**
 * El resultado de pasar un valor por la escala de una métrica.
 *
 * <p>Incluye la banda, no solo el número. Es la diferencia entre "tu NPS te dio
 * 2,0%" y "tu NPS de 55,4% cayó en la banda [53,2%, 57,3%), que paga 2,0%".
 * Sin la banda, el asesor recibe un número que no puede discutir; con la banda,
 * puede ver por qué le dio eso y cuánto le falta para el tramo de arriba.
 */
public record Alcance(Metrica metrica, BigDecimal valor, Banda banda, BigDecimal resultado) {

    static Alcance de(Metrica metrica, BigDecimal valor, Banda banda) {
        return new Alcance(metrica, valor, banda, banda.resultado());
    }
}
