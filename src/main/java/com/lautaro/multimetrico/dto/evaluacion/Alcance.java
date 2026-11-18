package com.lautaro.multimetrico.dto.evaluacion;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;

public record Alcance(Metrica metrica, BigDecimal valor, String banda, BigDecimal resultado) {
}
