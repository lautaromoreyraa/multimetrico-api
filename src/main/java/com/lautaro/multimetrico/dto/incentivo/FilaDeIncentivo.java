package com.lautaro.multimetrico.dto.incentivo;

import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import java.math.BigDecimal;

/**
 * Un asesor y lo que cobra, tal como sale por HTTP.
 *
 * <p>{@code importe} en {@code null} significa <b>que ese asesor no vino en la
 * planilla</b>, y no que cobre cero. Es la misma distinción que el multimétrico
 * hace entre una métrica sin archivo y una que paga cero, y por el mismo motivo:
 * escribir un 0 sería afirmar algo sobre la liquidación de una persona que nadie
 * afirmó.
 *
 * <p>Ojo con la asimetría respecto del multimétrico, que es deliberada: allá una
 * métrica sin archivo <b>bonifica cero</b> porque la bonificación es una regla
 * del pliego que se puede aplicar sin el dato. Acá no hay ninguna regla: el
 * importe llega calculado o no llega. Por eso el faltante se muestra vacío en vez
 * de contarse como cero, incluso en el total del equipo.
 */
public record FilaDeIncentivo(AsesorResponse asesor, BigDecimal importe) {

    public boolean tieneImporte() {
        return importe != null;
    }
}
