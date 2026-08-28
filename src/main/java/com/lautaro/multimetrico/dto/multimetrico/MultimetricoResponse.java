package com.lautaro.multimetrico.dto.multimetrico;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * El multimétrico de un equipo tal como sale por HTTP.
 *
 * <p>Reusa {@link ResumenDeEquipo} tal cual lo calculó el servicio en vez de
 * copiarlo campo por campo a un record propio: es exactamente el mismo dato, y
 * tener dos formas del resumen es tener dos lugares donde se puede olvidar
 * agregar un campo.
 */
public record MultimetricoResponse(
        YearMonth periodo,
        String campania,
        String lider,
        BigDecimal alcanceMaximo,
        ResumenDeEquipo resumen,
        List<FilaResponse> filas) {
}
