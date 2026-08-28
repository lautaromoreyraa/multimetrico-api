package com.lautaro.multimetrico.dto.padron;

/**
 * Una persona del padrón tal como sale por HTTP.
 *
 * <p>Hace doble tarea a propósito: es el cuerpo del listado del padrón y también
 * es la forma en que un asesor ya resuelto viaja dentro de una
 * {@code Coincidencia}. Son el mismo dato —legajo, nombre y líder— y duplicar el
 * record para que uno se llame distinto solo agregaría un lugar más donde los dos
 * pueden quedar desalineados.
 *
 * <p>Lo que no lleva es el {@code id} de la fila. La identidad de una persona en
 * este sistema es el legajo; el id es de la base y no tiene por qué salir.
 */
public record AsesorResponse(String legajo, String nombre, String lider) {
}
