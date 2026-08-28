package com.lautaro.multimetrico.dto.medicion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un valor con su fecha de corte, inseparables.
 *
 * <p>Que sean un solo objeto y no dos campos sueltos es el punto. Podría
 * devolverse el número a secas y alcanzaría para evaluar contra el pliego, pero
 * entonces se perdería el "actualizado al" de cada métrica, que es justo el dato
 * que la planilla pierde: junta un TMO al viernes con un FCR al lunes anterior y
 * los muestra como si fueran la misma foto. Acá el valor no se puede sacar sin su
 * corte.
 */
public record MedicionVigente(BigDecimal valor, LocalDate fechaDeCorte) {
}
