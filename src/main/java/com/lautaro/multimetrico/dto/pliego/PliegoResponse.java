package com.lautaro.multimetrico.dto.pliego;

import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.Unidad;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * El pliego tal como sale por HTTP.
 *
 * <p>Las bandas se devuelven ordenadas: el que las lee espera una tabla, no el
 * orden en que quedaron cargadas. De eso se encarga {@code PliegoMapper}.
 *
 * <p>Cada banda viaja con su {@code rango} ya armado —{@code "[669, 681)"}—
 * además de los dos bordes. No es redundancia: el texto tiene la convención del
 * intervalo semiabierto adentro, y dejar que cada pantalla lo redacte es
 * garantizar que alguna escriba {@code "669 a 681"} y pierda el detalle de que el
 * 681 no entra.
 */
public record PliegoResponse(
        YearMonth periodo,
        String campania,
        BigDecimal alcanceMaximo,
        List<EscalaResponse> escalas) {

    /**
     * La {@code unidad} viaja porque sin ella un 655 no se puede mostrar: el
     * TMO son segundos y las otras cuatro son porcentaje. El enum lo sabe desde
     * siempre; no exponerlo obligaba a cada pantalla a mantener su propio mapa
     * de metrica a unidad, que es duplicar algo que el servidor ya decidio.
     */
    public record EscalaResponse(
            Metrica metrica,
            String descripcion,
            String sentido,
            Unidad unidad,
            BigDecimal peso,
            List<BandaResponse> bandas) {
    }

    public record BandaResponse(BigDecimal desde, BigDecimal hasta, BigDecimal resultado, String rango) {
    }
}
