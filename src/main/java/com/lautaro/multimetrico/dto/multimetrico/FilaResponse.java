package com.lautaro.multimetrico.dto.multimetrico;

import com.lautaro.multimetrico.enumeration.CondicionDeTramo;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.Sentido;
import com.lautaro.multimetrico.enumeration.Unidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * La fila de un asesor tal como sale por HTTP.
 *
 * <p>Cada métrica sale con su banda, su fecha de corte y su tramo de arriba. La
 * banda va como texto —{@code "[75, +∞)"}— porque quien la lee la va a mostrar,
 * no operar con ella; el que necesita los bordes tiene el pliego.
 *
 * <p>Las métricas sin medición <b>no están en {@code metricas}</b>: están en
 * {@code faltantes}. Es a propósito, y es lo que obliga a la pantalla a decidir
 * qué hacer con ellas en vez de dejarlas caer de la tabla como hace la planilla.
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

    /**
     * La {@code unidad} viaja con la celda por la misma razon que el
     * {@code actualizadoAl}: sin ella el valor no se puede escribir. Un 655 de
     * TMO son segundos y un 655 no existe en las otras cuatro, que son
     * porcentaje. El enum {@link Metrica} lo sabe; mandarlo evita que la
     * pantalla mantenga su propio mapa y que los dos se separen.
     */
    public record CeldaResponse(
            Metrica metrica,
            String descripcion,
            Sentido sentido,
            Unidad unidad,
            BigDecimal valor,
            String banda,
            BigDecimal resultado,
            LocalDate actualizadoAl,
            TramoResponse tramoDeArriba) {
    }

    /**
     * Lo que le falta para el tramo de arriba, con el número y la frase.
     *
     * <p>El {@code detalle} va armado en el servidor y no en cada pantalla: es la
     * lectura correcta de la condición —alcanzar el borde alcanza, bajar de él
     * exige cruzarlo— y dejarla librada al frontend es garantizar que en algún
     * lado diga "te faltan 0" sin explicar por qué el asesor no está en la banda
     * de arriba.
     */
    public record TramoResponse(
            String banda,
            BigDecimal objetivo,
            BigDecimal distancia,
            CondicionDeTramo condicion,
            BigDecimal resultado,
            BigDecimal mejora,
            String detalle) {
    }
}
