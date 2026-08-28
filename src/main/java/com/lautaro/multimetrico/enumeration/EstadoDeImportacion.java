package com.lautaro.multimetrico.enumeration;

/** En qué punto está una importación. */
public enum EstadoDeImportacion {

    /** Se recibió el archivo y todavía se está procesando. */
    EN_CURSO,

    /** Procesó. Puede tener rechazos y pendientes; eso no la hace fallida. */
    TERMINADA,

    /**
     * El archivo no coincidía con el perfil y no se importó <b>nada</b>. Es
     * distinto de terminar con rechazos: acá el problema es del perfil o del
     * archivo entero, no de filas sueltas.
     */
    RECHAZADA
}
