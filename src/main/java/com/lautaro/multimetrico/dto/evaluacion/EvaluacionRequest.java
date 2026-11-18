package com.lautaro.multimetrico.dto.evaluacion;

import com.lautaro.multimetrico.enumeration.Metrica;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.Map;

public record EvaluacionRequest(
        @NotEmpty(message = "hace falta al menos una medición") Map<Metrica, BigDecimal> mediciones) {
}
