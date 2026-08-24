package com.lautaro.multimetrico.multimetrico.web;

import com.lautaro.multimetrico.multimetrico.domain.CeldaDeMultimetrico;
import com.lautaro.multimetrico.multimetrico.domain.FilaDeMultimetrico;
import com.lautaro.multimetrico.multimetrico.domain.TramoDeArriba;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import com.lautaro.multimetrico.pliego.domain.Sentido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * La fila de un asesor tal como sale por HTTP.
 *
 * <p>Cada métrica sale con su banda, su fecha de corte y su tramo de arriba. La
 * banda va como texto —{@code "[75, +∞)"}— porque quien la lee la va a mostrar,
 * no operar con ella; el que necesita los bordes tiene el pliego.
 */
public record FilaResponse(
        String legajo,
        String nombre,
        String lider,
        BigDecimal alcanceTotal,
        boolean completa,
        LocalDate actualizadoDesde,
        List<CeldaResponse> metricas,
        List<Metrica> faltantes,
        List<Metrica> fueraDeEscala) {

    public record CeldaResponse(
            Metrica metrica,
            String descripcion,
            Sentido sentido,
            BigDecimal valor,
            String banda,
            BigDecimal resultado,
            LocalDate actualizadoAl,
            TramoResponse tramoDeArriba) {
    }

    /**
     * Lo que le falta para el tramo de arriba, con el número y la frase.
     *
     * <p>La frase va armada acá y no en cada pantalla: es la lectura correcta de
     * la condición —alcanzar el borde alcanza, bajar de él exige cruzarlo— y
     * dejarla librada al frontend es garantizar que en algún lado diga "te faltan
     * 0" sin explicar por qué el asesor no está en la banda de arriba.
     */
    public record TramoResponse(
            String banda,
            BigDecimal objetivo,
            BigDecimal distancia,
            TramoDeArriba.Condicion condicion,
            BigDecimal resultado,
            BigDecimal mejora,
            String detalle) {
    }

    public static FilaResponse de(FilaDeMultimetrico fila) {
        return new FilaResponse(
                fila.asesor().legajo(),
                fila.asesor().nombre(),
                fila.asesor().lider(),
                fila.alcanceTotal(),
                fila.completa(),
                fila.corteMasViejo().orElse(null),
                fila.celdas().stream().map(FilaResponse::celda).toList(),
                fila.faltantes(),
                fila.fueraDeEscala());
    }

    private static CeldaResponse celda(CeldaDeMultimetrico celda) {
        return new CeldaResponse(
                celda.metrica(),
                celda.metrica().descripcion(),
                celda.metrica().sentido(),
                celda.alcance().valor(),
                celda.alcance().banda().rango(),
                celda.alcance().resultado(),
                celda.actualizadoAl(),
                celda.tramoDeArriba().map(tramo -> tramo(celda, tramo)).orElse(null));
    }

    private static TramoResponse tramo(CeldaDeMultimetrico celda, TramoDeArriba tramo) {
        BigDecimal mejora = tramo.mejora(celda.alcance());
        String detalle = switch (tramo.condicion()) {
            case ALCANZAR -> "faltan %s para llegar a %s y sumar %s".formatted(
                    tramo.distancia().toPlainString(), tramo.objetivo().toPlainString(), mejora.toPlainString());
            case BAJAR_DE -> "hay que quedar por debajo de %s —sobran %s— y sumar %s".formatted(
                    tramo.objetivo().toPlainString(), tramo.distancia().toPlainString(), mejora.toPlainString());
        };

        return new TramoResponse(
                tramo.banda().rango(),
                tramo.objetivo(),
                tramo.distancia(),
                tramo.condicion(),
                tramo.resultado(),
                mejora,
                detalle);
    }
}
