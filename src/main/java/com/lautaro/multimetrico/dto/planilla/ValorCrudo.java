package com.lautaro.multimetrico.dto.planilla;

import java.math.BigDecimal;

public record ValorCrudo(String texto, BigDecimal numero, boolean formatoPorcentaje) {

    private static final ValorCrudo VACIO = new ValorCrudo("", null, false);

    public ValorCrudo {
        texto = texto == null ? "" : texto.trim();
    }

    public static ValorCrudo ninguno() {
        return VACIO;
    }

    public static ValorCrudo deTexto(String texto) {
        return new ValorCrudo(texto, null, false);
    }

    public static ValorCrudo deNumero(double valor, boolean formatoPorcentaje, String comoSeVe) {
        return new ValorCrudo(comoSeVe, BigDecimal.valueOf(valor), formatoPorcentaje);
    }

    public boolean estaVacio() {
        return numero == null && texto.isBlank();
    }
}
