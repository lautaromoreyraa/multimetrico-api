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

    TMO("Tiempo medio de operación", Sentido.MENOR_ES_MEJOR, Unidad.SEGUNDOS),
    NPS("Net Promoter Score", Sentido.MAYOR_ES_MEJOR, Unidad.PORCENTAJE),
    FCR("Resolución en el primer contacto a 7 días", Sentido.MAYOR_ES_MEJOR, Unidad.PORCENTAJE),
    ACTIVIDAD_COMERCIAL("Actividad comercial", Sentido.MAYOR_ES_MEJOR, Unidad.PORCENTAJE),
    BAJA_CONVERGENTE("Porcentaje de baja convergente", Sentido.MENOR_ES_MEJOR, Unidad.PORCENTAJE);

    /**
     * En qué se mide.
     *
     * <p>Estaba implícito y no debería: quien pinta la pantalla necesita saber si
     * un 655 se muestra como "655 s" o como "655%", y sin esto lo tiene que
     * adivinar por el nombre de la métrica. El TMO es el único que no es
     * porcentaje — son segundos de conversación.
     */
    public enum Unidad {
        SEGUNDOS,
        PORCENTAJE
    }

    private final String descripcion;
    private final Sentido sentido;
    private final Unidad unidad;

    Metrica(String descripcion, Sentido sentido, Unidad unidad) {
        this.descripcion = descripcion;
        this.sentido = sentido;
        this.unidad = unidad;
    }

    public Unidad unidad() {
        return unidad;
    }

    public String descripcion() {
        return descripcion;
    }

    public Sentido sentido() {
        return sentido;
    }
}
