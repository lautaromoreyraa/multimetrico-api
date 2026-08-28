package com.lautaro.multimetrico.dto.planilla;

/**
 * Una fila de la planilla, ya ubicada por el perfil pero todavía sin interpretar.
 *
 * <p>El número de fila es el que se ve en Excel —empieza en 1—, no el índice
 * interno de POI. Es el dato que hace útil un rechazo: quien tiene que corregir
 * el archivo lo abre y va derecho a esa fila.
 *
 * <p>Es la frontera del módulo: de acá para adentro nadie sabe qué es una hoja de
 * cálculo. El día que la fuente publique CSV —o una API— se escribe otro lector y
 * no se toca nada más.
 */
public record FilaLeida(int numeroDeFila, String textoAsesor, ValorCrudo valor, String textoLider) {

    public boolean vacia() {
        return (textoAsesor == null || textoAsesor.isBlank()) && valor.estaVacio();
    }
}
