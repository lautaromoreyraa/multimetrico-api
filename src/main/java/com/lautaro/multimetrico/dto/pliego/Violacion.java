package com.lautaro.multimetrico.dto.pliego;

import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.TipoDeViolacion;

/**
 * Un problema concreto encontrado en un pliego.
 *
 * <p>El mensaje se escribe para que lo lea la persona que cargó el archivo, no
 * para un log: dice qué bandas, qué valores y qué consecuencia tiene. "Las
 * bandas se superponen" no sirve; "un TMO entre 680 y 681 cae en dos bandas"
 * manda a mirar la fila exacta.
 *
 * <p>Sale por dos caminos distintos y por eso está en {@code dto}: como cuerpo
 * del {@code 200} de la revisión —donde tener problemas es la respuesta
 * esperada— y adentro del {@code 422} de la carga. En los dos casos es lo que ve
 * quien tiene que corregir el pliego.
 */
public record Violacion(Metrica metrica, TipoDeViolacion tipo, String mensaje) {

    @Override
    public String toString() {
        return metrica == null ? mensaje : "%s: %s".formatted(metrica, mensaje);
    }
}
