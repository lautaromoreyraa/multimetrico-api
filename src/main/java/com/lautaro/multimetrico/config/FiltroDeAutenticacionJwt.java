package com.lautaro.multimetrico.config;

import com.lautaro.multimetrico.dto.seguridad.Credencial;
import com.lautaro.multimetrico.service.seguridad.ServicioDeTokens;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee el token del {@code Authorization} y deja al usuario en el contexto.
 *
 * <h2>No rechaza nada</h2>
 *
 * <p>Si no hay token, o el token no sirve, este filtro <b>sigue de largo</b> sin
 * autenticar a nadie. Quien devuelve el 401 es la cadena de seguridad, más
 * adelante, cuando descubre que la request llegó anónima a un endpoint que pide
 * autenticación.
 *
 * <p>Puede parecer un rodeo pero es lo que hace que los endpoints públicos —el
 * login— sigan funcionando con un token vencido en el navegador, y que el 401 lo
 * emita un solo lugar en vez de dos.
 *
 * <p>{@link OncePerRequestFilter} y no un filtro común: sin eso, un {@code
 * forward} interno vuelve a pasar por acá y el trabajo se hace dos veces.
 */
@Component
public class FiltroDeAutenticacionJwt extends OncePerRequestFilter {

    private static final String CABECERA = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final ServicioDeTokens tokens;

    public FiltroDeAutenticacionJwt(ServicioDeTokens tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        // Si ya hay alguien autenticado no se lo pisa. Importa para los tests,
        // que inyectan la autenticación directo sin pasar por un token.
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            tokenDe(request).flatMap(tokens::leer).ifPresent(credencial -> autenticar(credencial, request));
        }

        chain.doFilter(request, response);
    }

    private static java.util.Optional<String> tokenDe(HttpServletRequest request) {
        String cabecera = request.getHeader(CABECERA);
        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            return java.util.Optional.empty();
        }
        String token = cabecera.substring(PREFIJO.length()).trim();
        return token.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(token);
    }

    /**
     * El {@link Credencial} va como <em>principal</em>, no como un texto.
     *
     * <p>Así {@code ControlDeAcceso} lo saca del contexto sin volver a parsear
     * nada ni consultar la base: el rol y el equipo ya vinieron en el token.
     */
    private static void autenticar(Credencial credencial, HttpServletRequest request) {
        var autenticacion = new UsernamePasswordAuthenticationToken(
                credencial,
                null,
                List.of(new SimpleGrantedAuthority(credencial.rol().autoridad())));

        autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
