package com.lautaro.multimetrico.dto.incentivo;

public record FilaSinCargar(int numeroDeFila, String textoAsesor, String textoImporte, String motivo) {

    public FilaSinCargar {
        textoAsesor = textoAsesor == null ? "" : textoAsesor;
        textoImporte = textoImporte == null ? "" : textoImporte;
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Un rechazo sin motivo no explica nada");
        }
    }
}
