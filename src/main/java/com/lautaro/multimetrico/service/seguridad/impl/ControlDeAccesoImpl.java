package com.lautaro.multimetrico.service.seguridad.impl;

import com.lautaro.multimetrico.dto.seguridad.Credencial;
import com.lautaro.multimetrico.service.seguridad.ControlDeAcceso;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Lee la credencial del contexto de seguridad.
 *
 * <p>La dejó {@code FiltroDeAutenticacionJwt} como principal, así que acá no se
 * parsea nada ni se consulta la base: el legajo, el rol y el equipo ya vinieron
 * firmados en el token.
 */
@Service
public class ControlDeAccesoImpl implements ControlDeAcceso {

    @Override
    public Optional<Credencial> quienPide() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof Credencial credencial)) {
            return Optional.empty();
        }
        return Optional.of(credencial);
    }
}
