package com.lautaro.multimetrico.importacion.domain;

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
public record FilaRechazada(int numeroDeFila, String textoAsesor, String textoValor, Motivo motivo) {

    /**
     * Por qué no entró.
     *
     * <p>Todas describen la fila, no el archivo: un problema del archivo entero
     * —una hoja que no está, un encabezado que cambió— no rechaza filas, rechaza
     * la importación completa. Ver {@link Desajuste}.
     */
    public enum Motivo {

        /** Ninguna de las columnas del perfil tiene nada. Suele ser relleno al final de la hoja. */
        FILA_VACIA,

        /** Hay un valor pero no hay a quién atribuírselo. */
        ASESOR_VACIO,

        /**
         * Hay asesor pero la celda del valor está vacía.
         *
         * <p>No se importa como cero. Un cero es un desempeño malísimo y un
         * faltante es un dato que no llegó; el multimétrico los muestra distinto
         * a propósito.
         */
        VALOR_VACIO,

        /**
         * La celda del valor tiene algo que no es un número.
         *
         * <p>Este es el defecto que la planilla original tiene en cinco columnas
         * —la de la plata entre ellas—: texto y números en la misma columna, y
         * las sumas descartando filas sin decirlo.
         */
        VALOR_NO_NUMERICO
    }

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
