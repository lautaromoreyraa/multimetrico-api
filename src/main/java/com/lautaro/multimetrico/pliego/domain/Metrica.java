package com.lautaro.multimetrico.pliego.domain;

/**
 * Las métricas que componen el multimétrico.
 *
 * <p>Es un enum y no una tabla a propósito: el conjunto lo fija el negocio, no
 * el usuario. Un pliego nuevo cambia los umbrales todos los meses, pero no
 * inventa métricas — y si algún día aparece una, agregarla acá obliga a
 * decidir su sentido, que es justo lo que no conviene que quede implícito.
 */
public enum Metrica {

    TMO("Tiempo medio de operación", Sentido.MENOR_ES_MEJOR),
    NPS("Net Promoter Score", Sentido.MAYOR_ES_MEJOR),
    FCR("Resolución en el primer contacto a 7 días", Sentido.MAYOR_ES_MEJOR),
    ACTIVIDAD_COMERCIAL("Actividad comercial", Sentido.MAYOR_ES_MEJOR),
    BAJA_CONVERGENTE("Porcentaje de baja convergente", Sentido.MENOR_ES_MEJOR);

    private final String descripcion;
    private final Sentido sentido;

    Metrica(String descripcion, Sentido sentido) {
        this.descripcion = descripcion;
        this.sentido = sentido;
    }

    public String descripcion() {
        return descripcion;
    }

    public Sentido sentido() {
        return sentido;
    }
}
