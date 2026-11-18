package com.lautaro.multimetrico.dto.medicion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MedicionVigente(BigDecimal valor, LocalDate fechaDeCorte) {
}
