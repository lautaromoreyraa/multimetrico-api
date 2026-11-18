package com.lautaro.multimetrico.service.seguridad.impl;

import com.lautaro.multimetrico.domain.Usuario;
import com.lautaro.multimetrico.dto.seguridad.LoginRequest;
import com.lautaro.multimetrico.dto.seguridad.TokenResponse;
import com.lautaro.multimetrico.dto.seguridad.UsuarioRequest;
import com.lautaro.multimetrico.dto.seguridad.UsuarioResponse;
import com.lautaro.multimetrico.enumeration.Rol;
import com.lautaro.multimetrico.exception.CredencialesInvalidasException;
import com.lautaro.multimetrico.repository.UsuarioRepository;
import com.lautaro.multimetrico.service.seguridad.ServicioDeTokens;
import com.lautaro.multimetrico.service.seguridad.UsuarioService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final String HASH_DE_DESCARTE =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final String MENSAJE_UNICO = "Legajo o contraseña incorrectos.";

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final ServicioDeTokens tokens;

    public UsuarioServiceImpl(UsuarioRepository repositorio, PasswordEncoder encoder,
            ServicioDeTokens tokens) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse entrar(LoginRequest request) {
        Optional<Usuario> encontrado = repositorio.findByLegajoClave(Usuario.clave(request.legajo()));

        String hash = encontrado.map(Usuario::password).orElse(HASH_DE_DESCARTE);
        boolean coincide = encoder.matches(request.password(), hash);

        Usuario usuario = encontrado
                .filter(u -> coincide)
                .filter(Usuario::activo)
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_UNICO));

        return new TokenResponse(
                tokens.emitirPara(usuario),
                usuario.legajo(),
                usuario.nombre(),
                usuario.rol(),
                usuario.lider(),
                Instant.now().plus(tokens.minutosDeVigencia(), ChronoUnit.MINUTES));
    }

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (repositorio.existsByLegajoClave(Usuario.clave(request.legajo()))) {
            throw new IllegalArgumentException(
                    "Ya hay un usuario con el legajo %s.".formatted(request.legajo()));
        }

        if (request.rol() == Rol.SUPERVISOR && (request.lider() == null || request.lider().isBlank())) {
            throw new IllegalArgumentException(
                    "Un SUPERVISOR necesita el nombre del equipo que lidera, tal como figura en el padrón.");
        }

        Usuario creado = repositorio.save(new Usuario(
                request.legajo().trim(),
                request.nombre().trim(),
                encoder.encode(request.password()),
                request.rol(),
                request.lider() == null || request.lider().isBlank() ? null : request.lider().trim()));
        return aResponse(creado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repositorio.findAll().stream().map(UsuarioServiceImpl::aResponse).toList();
    }

    private static UsuarioResponse aResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.legajo(), usuario.nombre(), usuario.rol(), usuario.lider(), usuario.activo());
    }
}
