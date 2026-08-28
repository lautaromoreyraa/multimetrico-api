package com.lautaro.multimetrico.dto.evaluacion;

import com.lautaro.multimetrico.enumeration.Metrica;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Las mediciones de un asesor para un período.
 *
 * <p>Se acepta un mapa parcial a propósito: cada métrica se actualiza en su
 * propia fecha de corte, así que tener el TMO cargado y el FCR todavía no es el
 * estado normal a mitad de semana, no un error.
 */
public record EvaluacionRequest(
        @NotEmpty(message = "hace falta al menos una medición") Map<Metrica, BigDecimal> mediciones) {
}
