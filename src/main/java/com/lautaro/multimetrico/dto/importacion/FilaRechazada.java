package com.lautaro.multimetrico.dto.importacion;

import com.lautaro.multimetrico.enumeration.MotivoDeRechazo;

/**
 * Una fila que no se pudo importar, con el número de fila y el motivo.
 *
 * <p>El número de fila es el punto. "Se importaron 187 de 200" no sirve para
 * nada: la persona que subió el archivo no tiene forma de saber cuáles son las
 * trece que faltan. Con el número puede abrir la planilla, ir a esa fila y ver
 * qué le pasa. Es la diferencia entre un informe y un reproche.
 *
 * <p>Va <b>en base 1, como lo numera Excel.</b>
 *
 * <p>{@code textoAsesor} y {@code textoValor} se guardan crudos, tal como
 * estaban en las celdas, para que el motivo pueda citarlos. Un rechazo que dice
 * <em>"la columna del valor decía 'sin datos'"</em> se entiende sin abrir nada.
 */
public record FilaRechazada(int numeroDeFila, String textoAsesor, String textoValor, MotivoDeRechazo motivo) {

    public FilaRechazada {
        if (motivo == null) {
            throw new IllegalArgumentException("Un rechazo sin motivo no explica nada");
        }
        textoAsesor = textoAsesor == null ? "" : textoAsesor;
        textoValor = textoValor == null ? "" : textoValor;
    }

    /** El motivo escrito para que lo lea quien subió el archivo. */
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
