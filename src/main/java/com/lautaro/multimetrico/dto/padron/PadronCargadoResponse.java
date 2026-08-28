package com.lautaro.multimetrico.dto.padron;

import java.util.List;

/**
 * El acuse de una carga de padrón.
 *
 * <p>Devuelve la cantidad y la lista de líderes, no un "201 ok". Los dos números
 * son lo que permite darse cuenta en el acto de que se subió el archivo
 * equivocado: un padrón de 40 personas donde se esperaban 400, o una lista de
 * líderes donde falta uno, se ven sin abrir nada.
 *
 * @param asesores cuántas personas quedaron cargadas
 * @param lideres los líderes que aparecen, que son los nombres válidos para
 *     pedir un multimétrico de equipo
 */
public record PadronCargadoResponse(String periodo, int asesores, List<String> lideres) {
}
