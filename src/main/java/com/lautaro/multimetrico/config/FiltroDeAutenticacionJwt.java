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

    private static void autenticar(Credencial credencial, HttpServletRequest request) {
        var autenticacion = new UsernamePasswordAuthenticationToken(
                credencial,
                null,
                List.of(new SimpleGrantedAuthority(credencial.rol().autoridad())));

        autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
