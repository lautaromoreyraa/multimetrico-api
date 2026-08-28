package com.lautaro.multimetrico.dto.resolucion;

import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.enumeration.Estrategia;

/**
 * Un nombre del archivo, resuelto a una persona del padrón, y por qué vía.
 *
 * <p>La {@code estrategia} viaja pegada al resultado y no se descarta: cuando
 * alguien pregunte por qué una fila quedó asignada a determinada persona, la
 * respuesta tiene que estar guardada, no que reconstruirla.
 */
public record Coincidencia(String textoOriginal, AsesorResponse asesor, Estrategia estrategia) {
}
