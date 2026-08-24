package com.lautaro.multimetrico.multimetrico.domain;

import com.lautaro.multimetrico.pliego.domain.Alcance;
import com.lautaro.multimetrico.pliego.domain.Banda;
import com.lautaro.multimetrico.pliego.domain.Escala;
import com.lautaro.multimetrico.pliego.domain.Sentido;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;

/**
 * Cuánto le falta al asesor para pasar al tramo de arriba de una métrica.
 *
 * <p>Es lo único de todo el multimétrico que se puede accionar. El resto cuenta
 * lo que ya pasó; esto dice qué hacer: "estás en 71,4 de FCR y con 75 sumás 2
 * puntos más". Sale entero del pliego, sin ninguna regla nueva, y por eso vale
 * la pena calcularlo acá en vez de dejar que cada pantalla lo estime.
 *
 * <h2>Cuál es el tramo de arriba</h2>
 *
 * <p>No es "la banda siguiente en la lista", porque hacia dónde se mejora
 * depende de {@link Sentido}: en NPS o FCR se sube y en TMO o bajas se baja. Y
 * tampoco se supone que el resultado crezca de forma pareja con el valor —eso es
 * cierto en los pliegos sanos pero nada lo obliga—, así que primero se filtran
 * las bandas que <b>pagan más que la actual</b> y recién después se busca, entre
 * ésas, la que queda más cerca en la dirección en la que la métrica mejora. Si
 * no hay ninguna, no hay tramo de arriba: el asesor ya está en el mejor.
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
 */
public record TramoDeArriba(Banda banda, BigDecimal objetivo, BigDecimal distancia, Condicion condicion) {

    /** Qué hay que hacer con el objetivo para entrar en la banda. */
    public enum Condicion {
        /** Llegar al objetivo o pasarlo. El borde de abajo de la banda entra. */
        ALCANZAR,
        /** Quedar estrictamente por debajo del objetivo. El borde de arriba no entra. */
        BAJAR_DE
    }

    /** Lo que paga la banda de arriba. */
    public BigDecimal resultado() {
        return banda.resultado();
    }

    /** Cuánto más suma el asesor si llega: la diferencia con lo que cobra hoy. */
    public BigDecimal mejora(Alcance actual) {
        return resultado().subtract(actual.resultado());
    }

    /**
     * El tramo de arriba de un alcance ya calculado, o vacío si no hay ninguno
     * mejor al alcance.
     *
     * <p>Vacío significa una de dos cosas: el asesor está en la mejor banda de
     * la escala, o el pliego tiene las bandas que pagan más del lado equivocado
     * —lo que sería un pliego raro, no un error de este cálculo—. En los dos
     * casos lo honesto es no mostrar nada antes que mostrar un objetivo que no
     * mejora.
     */
    public static Optional<TramoDeArriba> de(Escala escala, Alcance alcance) {
        BigDecimal valor = alcance.valor();

        var mejores = escala.bandas().stream()
                .filter(banda -> banda.resultado().compareTo(alcance.resultado()) > 0);

        if (escala.metrica().sentido() == Sentido.MAYOR_ES_MEJOR) {
            return mejores
                    // Una banda abierta hacia abajo nunca está "más arriba": su desde es null.
                    .filter(banda -> banda.desde() != null && banda.desde().compareTo(valor) > 0)
                    .min(Comparator.comparing(Banda::desde))
                    .map(banda -> new TramoDeArriba(banda, banda.desde(),
                            banda.desde().subtract(valor), Condicion.ALCANZAR));
        }

        return mejores
                // hasta <= valor, no <: si el valor cae justo en el borde, todavía
                // está del lado de afuera y esa banda sigue siendo la de arriba.
                .filter(banda -> banda.hasta() != null && banda.hasta().compareTo(valor) <= 0)
                .max(Comparator.comparing(Banda::hasta))
                .map(banda -> new TramoDeArriba(banda, banda.hasta(),
                        valor.subtract(banda.hasta()), Condicion.BAJAR_DE));
    }
}
