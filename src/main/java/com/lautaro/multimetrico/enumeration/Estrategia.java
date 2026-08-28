package com.lautaro.multimetrico.enumeration;

/**
 * Cómo se resolvió un nombre contra el padrón.
 *
 * <p>Viaja en el resultado a propósito. Cuando alguien pregunte por qué una fila
 * quedó asignada a determinada persona, la respuesta tiene que estar guardada, no
 * que reconstruirla. Y si aparecen muchas resoluciones por {@link #PREFIJO_UNICO},
 * eso ya dice algo: el archivo viene truncando nombres.
 */
public enum Estrategia {

    /** El archivo traía el legajo. Es el único caso sin ambigüedad posible. */
    LEGAJO,

    /** El nombre normalizado coincide exacto con el del padrón. */
    NOMBRE_EXACTO,

    /** El nombre venía cortado y es principio de un único asesor del padrón. */
    PREFIJO_UNICO,

    /** Una persona ya había confirmado antes que este texto es ese legajo. */
    ALIAS_CONFIRMADO
}
