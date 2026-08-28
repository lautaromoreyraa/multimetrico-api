package com.lautaro.multimetrico.dto.padron;

/**
 * Un asesor del padrón ofrecido para que una persona lo confirme.
 *
 * <p>Aparece en los dos informes que muestran nombres sin resolver —el de
 * resolución del padrón y el de una importación— y es el mismo dato en los dos.
 *
 * <p><b>Siempre es una sugerencia, nunca una resolución.</b> Si el sistema
 * pudiera decidir, la fila no sería un pendiente. En un cálculo que termina en
 * cuánto cobra alguien, un parecido no alcanza: adivinar mal le atribuye a una
 * persona las métricas de otra y no queda rastro de que fue una suposición.
 */
public record Candidato(String legajo, String nombre) {
}
