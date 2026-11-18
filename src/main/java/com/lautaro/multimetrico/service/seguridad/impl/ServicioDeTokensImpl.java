package com.lautaro.multimetrico.service.seguridad.impl;

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

public class ServicioDeTokensImpl implements ServicioDeTokens {

    private static final Logger log = LoggerFactory.getLogger(ServicioDeTokensImpl.class);

    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_LIDER = "lider";

    private final SecretKey clave;
    private final long minutosDeVigencia;
    private final String emisor;

    public ServicioDeTokensImpl(String secreto, long minutosDeVigencia, String emisor) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.minutosDeVigencia = minutosDeVigencia;
        this.emisor = emisor;
    }

    @Override
    public long minutosDeVigencia() {
        return minutosDeVigencia;
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
            log.debug("Token rechazado: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
