package com.lautaro.multimetrico.service.seguridad.impl;

import com.lautaro.multimetrico.config.PropiedadesDeSeguridad;
import com.lautaro.multimetrico.domain.Usuario;
import com.lautaro.multimetrico.dto.seguridad.Credencial;
import com.lautaro.multimetrico.enumeration.Rol;
import com.lautaro.multimetrico.service.seguridad.ServicioDeTokens;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Tokens firmados con HS256.
 *
 * <h2>Por qué HS256 y no un par de claves</h2>
 *
 * <p>Hay un solo servicio que emite y verifica. Una clave asimétrica sirve cuando
 * quien verifica no es quien firma —varios servicios, o un cliente que valida por
 * su cuenta— y acá no es el caso. Una clave compartida es más simple y no hay
 * nada que la simplicidad esté escondiendo.
 *
 * <h2>Qué viaja adentro</h2>
 *
 * <p>El legajo en el {@code subject}, y el rol y el líder como claims. Nada más:
 * un token es un dato que el cliente puede leer —está firmado, no cifrado— así
 * que no entra ahí nada que no se le pueda mostrar a quien lo tiene.
 *
 * <p>El líder es la parte incómoda: es el equipo del supervisor, y va en el token
 * para no consultar la base en cada request. Ver {@link Credencial} para lo que
 * eso implica cuando alguien cambia de equipo.
 */
@Service
public class ServicioDeTokensImpl implements ServicioDeTokens {

    private static final Logger log = LoggerFactory.getLogger(ServicioDeTokensImpl.class);

    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_LIDER = "lider";

    private final SecretKey clave;
    private final long minutosDeVigencia;
    private final String emisor;

    public ServicioDeTokensImpl(PropiedadesDeSeguridad propiedades) {
        // La validación del secreto ya corrió en PropiedadesDeSeguridad: si
        // llegamos acá, es largo y no está vacío.
        this.clave = Keys.hmacShaKeyFor(
                propiedades.getJwt().getSecreto().getBytes(StandardCharsets.UTF_8));
        this.minutosDeVigencia = propiedades.getJwt().getMinutosDeVigencia();
        this.emisor = propiedades.getJwt().getEmisor();
    }

    @Override
    public String emitirPara(Usuario usuario) {
        Instant ahora = Instant.now();

        return Jwts.builder()
                .issuer(emisor)
                .subject(usuario.legajo())
                .claim(CLAIM_ROL, usuario.rol().name())
                .claim(CLAIM_LIDER, usuario.lider())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(minutosDeVigencia, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    @Override
    public Optional<Credencial> leer(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .requireIssuer(emisor)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(new Credencial(
                    claims.getSubject(),
                    Rol.valueOf(claims.get(CLAIM_ROL, String.class)),
                    claims.get(CLAIM_LIDER, String.class)));

        } catch (JwtException | IllegalArgumentException e) {
            // Firma inválida, vencido, emisor ajeno, rol que ya no existe, o
            // directamente basura. Son todos el mismo caso desde afuera —no se
            // le puede creer— y distinguirlos en la respuesta le diría a quien
            // prueba tokens cuál de sus intentos estuvo más cerca.
            log.debug("Token rechazado: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
