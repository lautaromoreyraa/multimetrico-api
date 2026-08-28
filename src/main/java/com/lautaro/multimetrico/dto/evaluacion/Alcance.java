package com.lautaro.multimetrico.dto.evaluacion;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;

/**
 * El resultado de pasar un valor por la escala de una métrica.
 *
 * <p>Incluye la banda, no solo el número. Es la diferencia entre "tu NPS te dio
 * 2,0%" y "tu NPS de 55,4% cayó en la banda [53,2%, 57,3%), que paga 2,0%".
 * Sin la banda, el asesor recibe un número que no puede discutir; con la banda,
 * puede ver por qué le dio eso y cuánto le falta para el tramo de arriba.
 *
 * <p>La banda viaja como el texto de su rango y no como la entidad: quien lee un
 * alcance la va a mostrar, no operar con ella. El que necesita los bordes para
 * calcular tiene el pliego, que es de donde salen.
 *
 * @param valor lo que midió el asesor
 * @param banda el rango en que cayó, ya redactado: {@code "[53.2, 57.3)"}
 * @param resultado lo que paga esa banda
 */
public record Alcance(Metrica metrica, BigDecimal valor, String banda, BigDecimal resultado) {
}
