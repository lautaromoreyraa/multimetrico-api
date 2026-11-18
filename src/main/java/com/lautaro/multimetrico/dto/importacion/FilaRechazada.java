package com.lautaro.multimetrico.dto.importacion;

import com.lautaro.multimetrico.enumeration.MotivoDeRechazo;

public record FilaRechazada(int numeroDeFila, String textoAsesor, String textoValor, MotivoDeRechazo motivo) {

    public FilaRechazada {
        if (motivo == null) {
            throw new IllegalArgumentException("Un rechazo sin motivo no explica nada");
        }
        textoAsesor = textoAsesor == null ? "" : textoAsesor;
        textoValor = textoValor == null ? "" : textoValor;
    }

    public String explicacion() {
        return switch (motivo) {
            case FILA_VACIA -> "la fila está vacía en todas las columnas del perfil";
            case ASESOR_VACIO -> "hay un valor (%s) pero la columna del asesor está vacía".formatted(textoValor);
            case VALOR_VACIO -> "la columna del valor está vacía; una métrica que no llegó queda faltante, "
                    + "no se importa en cero";
            case VALOR_NO_NUMERICO -> "la columna del valor decía '%s', que no es un número".formatted(textoValor);
        };
    }
}
