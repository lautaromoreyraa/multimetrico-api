package com.lautaro.multimetrico.service.seguridad;

import com.lautaro.multimetrico.dto.seguridad.Credencial;
import java.util.Optional;

/**
 * De quién es la fila que se está pidiendo.
 *
 * <h2>Por qué esto existe y no alcanza con los roles</h2>
 *
 * <p>La cadena de seguridad decide a qué endpoint se llega, y eso un rol lo puede
 * contestar. Lo que un rol no puede contestar es de quién es el dato: dos
 * supervisores tienen el mismo rol y equipos distintos, y
 * {@code GET /api/multimetrico/2026-08/asesores/10412} es el mismo endpoint sin
 * importar si el 10412 sos vos o es otro.
 *
 * <p>El requisito del proyecto está escrito así: <b>un asesor que pide la fila de
 * otro recibe 403, nunca la fila</b>. "Nunca la fila" es la parte importante —no
 * alcanza con esconderla en la pantalla— y por eso el control vive en el
 * servicio, del lado de adentro, y no en un filtro que mira la URL.
 *
 * <h2>Quién ve qué</h2>
 *
 * <ul>
 *   <li><b>ADMIN</b>: todo.</li>
 *   <li><b>SUPERVISOR</b>: las filas de su equipo, y la suya propia.</li>
 *   <li><b>ASESOR</b>: su fila y nada más.</li>
 * </ul>
 */
public interface ControlDeAcceso {

    /** Quién está pidiendo, o vacío si la request llegó sin autenticar. */
    Optional<Credencial> quienPide();

    /**
     * Falla con 403 si quien pide no puede ver la fila de ese legajo.
     *
     * <p>Devuelve {@code void} y lanza, en vez de devolver un booleano, a
     * propósito: un booleano se puede ignorar y el compilador no dice nada. Un
     * método que lanza obliga a llamarlo antes de leer, que es el orden correcto.
     */
    void exigirAccesoA(String legajo);

    /**
     * Falla con 403 si quien pide no puede ver el multimétrico de ese líder.
     *
     * <p>{@code lider} en {@code null} es "toda la operación", que sólo un ADMIN
     * puede pedir: un supervisor que pide la operación entera está pidiendo los
     * equipos de los demás.
     */
    void exigirAccesoAlEquipo(String lider);

    /**
     * El líder al que hay que acotar una consulta de equipo.
     *
     * <p>Existe para el caso del supervisor que pide "toda la operación": en vez
     * de rechazarlo, la consulta se acota sola a su equipo. Es más útil que un
     * 403 y no filtra nada, porque el resultado es exactamente lo que ya tenía
     * derecho a ver.
     */
    String liderEfectivo(String liderPedido);
}
