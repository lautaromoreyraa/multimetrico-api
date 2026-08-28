package com.lautaro.multimetrico.enumeration;

/**
 * En qué se mide una métrica.
 *
 * <p>Estaba implícito y no debería: quien pinta la pantalla necesita saber si un
 * 655 se muestra como "655 s" o como "655%", y sin esto lo tiene que adivinar
 * por el nombre de la métrica. El TMO es el único que no es porcentaje —son
 * segundos de conversación—.
 */
public enum Unidad {
    SEGUNDOS,
    PORCENTAJE
}
