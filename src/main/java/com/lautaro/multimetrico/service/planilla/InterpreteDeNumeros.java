package com.lautaro.multimetrico.service.planilla;

import com.lautaro.multimetrico.domain.FormatoDeNumero;
import com.lautaro.multimetrico.dto.planilla.ValorCrudo;
import com.lautaro.multimetrico.enumeration.EscalaDeNumero;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;
import com.lautaro.multimetrico.util.DecimalUtil;
import java.math.BigDecimal;
import java.util.Optional;

public final class InterpreteDeNumeros {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private static final String RUIDO = "[\\s\\u00A0%$]";

    private static final String UN_NUMERO = "[+-]?\\d+(\\.\\d+)?";

    private InterpreteDeNumeros() {
    }

    public static Optional<BigDecimal> interpretar(FormatoDeNumero formato, ValorCrudo crudo) {
        if (crudo == null || crudo.estaVacio()) {
            return Optional.empty();
        }
        if (crudo.numero() != null) {
            return Optional.of(crudo.formatoPorcentaje()
                    ? limpiar(crudo.numero().multiply(CIEN))
                    : escalar(formato, crudo.numero()));
        }
        boolean signoEscrito = crudo.texto().contains("%");
        return parsear(formato, crudo.texto())
                .map(numero -> signoEscrito ? numero : escalar(formato, numero));
    }

    private static BigDecimal escalar(FormatoDeNumero formato, BigDecimal numero) {
        return formato.escala() == EscalaDeNumero.FRACCION_DE_UNO
                ? limpiar(numero.multiply(CIEN))
                : numero;
    }

    private static BigDecimal limpiar(BigDecimal numero) {
        return DecimalUtil.comoEntro(numero);
    }

    private static Optional<BigDecimal> parsear(FormatoDeNumero formato, String texto) {
        String limpio = texto.replaceAll(RUIDO, "");

        limpio = formato.separadorDecimal() == SeparadorDecimal.COMA
                ? limpio.replace(".", "").replace(',', '.')
                : limpio.replace(",", "");

        if (!limpio.matches(UN_NUMERO)) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(limpio));
    }
}
