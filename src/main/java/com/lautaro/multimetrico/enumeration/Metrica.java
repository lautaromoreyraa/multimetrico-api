package com.lautaro.multimetrico.enumeration;

public enum Metrica {

    TMO("Tiempo medio de operación", Sentido.MENOR_ES_MEJOR, Unidad.SEGUNDOS),
    NPS("Net Promoter Score", Sentido.MAYOR_ES_MEJOR, Unidad.PORCENTAJE),
    FCR("Resolución en el primer contacto a 7 días", Sentido.MAYOR_ES_MEJOR, Unidad.PORCENTAJE),
    ACTIVIDAD_COMERCIAL("Actividad comercial", Sentido.MAYOR_ES_MEJOR, Unidad.PORCENTAJE),
    BAJA_CONVERGENTE("Porcentaje de baja convergente", Sentido.MENOR_ES_MEJOR, Unidad.PORCENTAJE);

    private final String descripcion;
    private final Sentido sentido;
    private final Unidad unidad;

    Metrica(String descripcion, Sentido sentido, Unidad unidad) {
        this.descripcion = descripcion;
        this.sentido = sentido;
        this.unidad = unidad;
    }

    public String descripcion() {
        return descripcion;
    }

    public Sentido sentido() {
        return sentido;
    }

    public Unidad unidad() {
        return unidad;
    }
}
