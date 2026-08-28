package com.lautaro.multimetrico.enumeration;

/**
 * Por qué una fila del archivo no entró.
 *
 * <p>Todas describen la fila, no el archivo: un problema del archivo entero —una
 * hoja que no está, un encabezado que cambió— no rechaza filas, rechaza la
 * importación completa. Ver {@code Desajuste}.
 */
public enum MotivoDeRechazo {

    /** Ninguna de las columnas del perfil tiene nada. Suele ser relleno al final de la hoja. */
    FILA_VACIA,

    /** Hay un valor pero no hay a quién atribuírselo. */
    ASESOR_VACIO,

    /**
     * Hay asesor pero la celda del valor está vacía.
     *
     * <p>No se importa como cero. Un cero es un desempeño malísimo y un faltante
     * es un dato que no llegó; el multimétrico los muestra distinto a propósito.
     */
    VALOR_VACIO,

    /**
     * La celda del valor tiene algo que no es un número.
     *
     * <p>Este es el defecto que la planilla original tiene en cinco columnas —la
     * de la plata entre ellas—: texto y números en la misma columna, y las sumas
     * descartando filas sin decirlo.
     */
    VALOR_NO_NUMERICO
}
