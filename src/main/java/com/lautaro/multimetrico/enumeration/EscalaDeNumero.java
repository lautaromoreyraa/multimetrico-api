package com.lautaro.multimetrico.enumeration;

/**
 * En qué unidad viene el número de un archivo cuando la celda no lo declara.
 *
 * <p>Elegir mal no rompe nada de manera visible: produce un número cien veces
 * más grande o más chico, que cae en otra banda y sale por la API como si fuera
 * cierto. Por eso la unidad es parte del perfil y no una suposición del código.
 */
public enum EscalaDeNumero {

    /** El número ya está en las unidades del pliego: 80,5 es 80,5. */
    TAL_CUAL,

    /** El número es una fracción de uno: 0,805 son 80,5 puntos. Se multiplica por cien. */
    FRACCION_DE_UNO
}
