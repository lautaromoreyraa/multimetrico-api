package com.lautaro.multimetrico.dto.multimetrico;

import com.lautaro.multimetrico.enumeration.CondicionDeTramo;
import com.lautaro.multimetrico.enumeration.Sentido;
import java.math.BigDecimal;

/**
 * Cuánto le falta al asesor para pasar al tramo de arriba de una métrica.
 *
 * <p>Es lo único de todo el multimétrico que se puede accionar. El resto cuenta
 * lo que ya pasó; esto dice qué hacer: "estás en 71,4 de FCR y con 75 sumás 2
 * puntos más". Sale entero del pliego, sin ninguna regla nueva, y por eso vale
 * la pena calcularlo en el servidor en vez de dejar que cada pantalla lo estime.
 *
 * <h2>Cuál es el tramo de arriba</h2>
 *
 * <p>No es "la banda siguiente en la lista", porque hacia dónde se mejora
 * depende de {@link Sentido}: en NPS o FCR se sube y en TMO o bajas se baja. Y
 * tampoco se supone que el resultado crezca de forma pareja con el valor —eso es
 * cierto en los pliegos sanos pero nada lo obliga—. El criterio completo está en
 * {@code MultimetricoServiceImpl}, que es quien arma este objeto.
 *
 * <h2>Por qué la condición viaja aparte de la distancia</h2>
 *
 * <p>Las bandas son intervalos semiabiertos {@code [desde, hasta)}, y eso hace
 * que los dos bordes no se alcancen igual:
 *
 * <ul>
 *   <li>Subiendo, el objetivo es el {@code desde} de la banda de arriba y ese
 *       borde <b>entra</b>: llegar justo a 75 ya cuenta. La distancia es exacta.</li>
 *   <li>Bajando, el objetivo es el {@code hasta} de la banda de abajo y ese
 *       borde <b>no entra</b>: con exactamente 600 todavía se está en la banda
 *       de 600 a 700, hay que quedar por debajo. La distancia sigue siendo la
 *       que hay hasta el borde —no se inventa un epsilon, que sería mentir con
 *       una precisión que el pliego no tiene—, y la condición es la que avisa
 *       que ese último paso hay que darlo.</li>
 * </ul>
 *
 * <p>De ahí que un asesor que está clavado en el borde vea distancia 0 con
 * condición {@code BAJAR_DE}. Es la lectura correcta: no le falta nada medible,
 * le falta cruzar.
 *
 * @param banda el rango de la banda de arriba, ya redactado
 * @param resultado lo que paga esa banda
 * @param mejora cuánto más suma el asesor si llega: la diferencia con lo que cobra hoy
 */
public record TramoDeArriba(
        String banda,
        BigDecimal objetivo,
        BigDecimal distancia,
        CondicionDeTramo condicion,
        BigDecimal resultado,
        BigDecimal mejora) {
}
