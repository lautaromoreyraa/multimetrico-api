package com.lautaro.multimetrico.service.seguridad;

import com.lautaro.multimetrico.dto.seguridad.Credencial;
import java.util.Optional;

/**
 * Quién está haciendo esta request.
 *
 * <h2>Antes hacía más</h2>
 *
 * <p>Tenía el control por fila que pedía el PLAN §5.6 —un asesor sólo ve la
 * suya— y se sacó cuando la lectura pasó a ser pública. Queda anotado porque no
 * fue un descarte: la infraestructura sigue entera y {@code Rol} sigue
 * distinguiendo ASESOR de SUPERVISOR, así que reponerlo es volver a llamar al
 * control desde {@code MultimetricoServiceImpl}, no escribirlo de nuevo.
 *
 * <p>Hoy sólo contesta quién es el dueño del token, que es lo que el panel de
 * administración necesita para saber si la sesión sigue viva.
 */
public interface ControlDeAcceso {

    /** Quién pide, o vacío si la request llegó sin autenticar. */
    Optional<Credencial> quienPide();
}
