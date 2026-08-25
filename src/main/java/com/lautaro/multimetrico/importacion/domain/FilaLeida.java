package com.lautaro.multimetrico.importacion.domain;

/**
 * Una fila de la planilla, ya ubicada por el perfil pero todavía sin interpretar.
 *
 * <p>El número de fila es el que se ve en Excel —empieza en 1—, no el índice
 * interno de POI. Es el dato que hace útil un rechazo: quien tiene que corregir
 * el archivo lo abre y va derecho a esa fila.
 */
public record FilaLeida(int numeroDeFila, String textoAsesor, ValorCrudo valor, String textoLider) {

    public boolean vacia() {
        return (textoAsesor == null || textoAsesor.isBlank()) && valor.estaVacio();
    }
}
