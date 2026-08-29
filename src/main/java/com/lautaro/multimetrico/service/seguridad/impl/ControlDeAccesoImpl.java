package com.lautaro.multimetrico.service.seguridad.impl;

import com.lautaro.multimetrico.dto.seguridad.Credencial;
import com.lautaro.multimetrico.exception.AccesoDenegadoException;
import com.lautaro.multimetrico.service.seguridad.ControlDeAcceso;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * El control por fila, leyendo del contexto de seguridad.
 *
 * <p>La credencial la dejó {@code FiltroDeAutenticacionJwt} como principal, así
 * que acá no se parsea nada ni se consulta la base: el legajo, el rol y el equipo
 * ya vinieron firmados en el token.
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

    @Override
    public void exigirAccesoA(String legajo) {
        Credencial quien = exigirAutenticado();

        if (quien.esAdmin() || mismoLegajo(quien.legajo(), legajo)) {
            return;
        }

        // Un supervisor ve a los suyos, pero de quién es cada legajo lo dice el
        // padrón y no el token. Resolverlo acá obligaría a este control a leer el
        // padrón del período —que no conoce— así que la comprobación de equipo la
        // hace quien ya tiene la fila en la mano: MultimetricoServiceImpl, que
        // compara el líder de la fila contra el del supervisor.
        if (quien.esSupervisor()) {
            return;
        }

        throw new AccesoDenegadoException(
                "El legajo %s no es tuyo. Un asesor sólo ve su propia fila.".formatted(legajo));
    }

    @Override
    public void exigirAccesoAlEquipo(String lider) {
        Credencial quien = exigirAutenticado();

        if (quien.esAdmin()) {
            return;
        }

        if (quien.esSupervisor()) {
            // liderEfectivo ya lo acotó a su equipo; si igual llegó otro nombre,
            // está pidiendo el equipo de otro.
            if (lider != null && mismoTexto(lider, quien.lider())) {
                return;
            }
            throw new AccesoDenegadoException(
                    "El equipo de %s no es el tuyo.".formatted(lider));
        }

        throw new AccesoDenegadoException(
                "Un asesor no ve la tabla del equipo. Mirá tus métricas en /api/multimetrico/{periodo}/asesores/"
                        + quien.legajo());
    }

    @Override
    public String liderEfectivo(String liderPedido) {
        Credencial quien = exigirAutenticado();

        if (quien.esAdmin()) {
            return liderPedido;
        }

        // Un supervisor que no pide nada en particular quiere el suyo. Acotarlo
        // es mejor que rechazarlo: el resultado es exactamente lo que ya tenía
        // derecho a ver.
        if (quien.esSupervisor() && (liderPedido == null || liderPedido.isBlank())) {
            return quien.lider();
        }

        return liderPedido;
    }

    private Credencial exigirAutenticado() {
        return quienPide().orElseThrow(() -> new AccesoDenegadoException(
                "Esta operación necesita un usuario autenticado."));
    }

    private static boolean mismoLegajo(String uno, String otro) {
        return uno != null && otro != null && uno.trim().equalsIgnoreCase(otro.trim());
    }

    private static boolean mismoTexto(String uno, String otro) {
        return uno != null && otro != null
                && uno.trim().toUpperCase(Locale.ROOT).equals(otro.trim().toUpperCase(Locale.ROOT));
    }
}
