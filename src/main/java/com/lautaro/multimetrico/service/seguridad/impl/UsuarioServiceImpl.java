package com.lautaro.multimetrico.service.seguridad.impl;

import com.lautaro.multimetrico.config.PropiedadesDeSeguridad;
import com.lautaro.multimetrico.domain.Usuario;
import com.lautaro.multimetrico.dto.seguridad.LoginRequest;
import com.lautaro.multimetrico.dto.seguridad.TokenResponse;
import com.lautaro.multimetrico.dto.seguridad.UsuarioRequest;
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

/**
 * Entrar y dar de alta usuarios.
 *
 * <h2>El login no dice qué falló</h2>
 *
 * <p>Legajo inexistente, contraseña equivocada y usuario desactivado devuelven el
 * mismo mensaje. Distinguirlos convertiría al login en una forma de averiguar qué
 * legajos existen, que es la mitad del trabajo de quien después prueba
 * contraseñas.
 *
 * <p>Por la misma razón se verifica la contraseña <b>aunque el usuario no
 * exista</b>: comparar contra un hash de descarte hace que las dos respuestas
 * tarden parecido. Sin eso, un "no existe" vuelve mucho más rápido que un
 * "contraseña mal", y esa diferencia de tiempo dice lo mismo que el mensaje que
 * se decidió no dar.
 */
@Service
public class UsuarioServiceImpl implements UsuarioService {

    /**
     * Un hash real contra el que comparar cuando el legajo no existe.
     *
     * <p>Es el BCrypt de una contraseña que nadie tiene. Su único trabajo es
     * gastar el mismo tiempo que gastaría una comparación de verdad.
     */
    private static final String HASH_DE_DESCARTE =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final String MENSAJE_UNICO = "Legajo o contraseña incorrectos.";

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final ServicioDeTokens tokens;
    private final long minutosDeVigencia;

    public UsuarioServiceImpl(UsuarioRepository repositorio, PasswordEncoder encoder,
            ServicioDeTokens tokens, PropiedadesDeSeguridad propiedades) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.tokens = tokens;
        this.minutosDeVigencia = propiedades.getJwt().getMinutosDeVigencia();
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse entrar(LoginRequest request) {
        Optional<Usuario> encontrado = repositorio.findByLegajoClave(Usuario.clave(request.legajo()));

        // Siempre se compara, exista o no: ver el javadoc de la clase.
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
                Instant.now().plus(minutosDeVigencia, ChronoUnit.MINUTES));
    }

    @Override
    @Transactional
    public Usuario crear(UsuarioRequest request) {
        if (repositorio.existsByLegajoClave(Usuario.clave(request.legajo()))) {
            throw new IllegalArgumentException(
                    "Ya hay un usuario con el legajo %s.".formatted(request.legajo()));
        }

        // Un supervisor sin equipo no ve a nadie, y eso casi siempre es un olvido
        // al darlo de alta y no una intención. Se rechaza al crearlo, que es
        // cuando se puede corregir, en vez de dejar que se entere el día que
        // entra y ve una tabla vacía.
        if (request.rol() == Rol.SUPERVISOR && (request.lider() == null || request.lider().isBlank())) {
            throw new IllegalArgumentException(
                    "Un SUPERVISOR necesita el nombre del equipo que lidera, tal como figura en el padrón.");
        }

        return repositorio.save(new Usuario(
                request.legajo().trim(),
                request.nombre().trim(),
                encoder.encode(request.password()),
                request.rol(),
                request.lider() == null || request.lider().isBlank() ? null : request.lider().trim()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repositorio.findAll();
    }
}
