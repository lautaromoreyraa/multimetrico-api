package com.lautaro.multimetrico.dto.planilla;

public record FilaLeida(int numeroDeFila, String textoAsesor, ValorCrudo valor, String textoLider) {

    public boolean vacia() {
        return (textoAsesor == null || textoAsesor.isBlank()) && valor.estaVacio();
    }
}
