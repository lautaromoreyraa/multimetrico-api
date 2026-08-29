package com.lautaro.multimetrico.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lautaro.multimetrico.dto.error.ErrorResponse;
import com.lautaro.multimetrico.enumeration.Rol;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Quién llega a cada endpoint.
 *
 * <h2>Esto es sólo la mitad del control</h2>
 *
 * <p>Acá se decide <b>a qué endpoint</b> se llega, que es lo que un rol puede
 * contestar. Lo que un rol <b>no</b> puede contestar es de quién es la fila que
 * se está pidiendo: dos supervisores tienen el mismo rol y equipos distintos.
 *
 * <p>Esa segunda mitad vive en {@code ControlDeAcceso} y es la que importa. El
 * requisito del proyecto está escrito así: <i>un asesor que pide la fila de otro
 * recibe 403, nunca la fila</i>. Una regla por endpoint no puede cumplirlo —el
 * endpoint es el mismo— y por eso el control es por fila.
 *
 * <h2>Sin sesión y sin CSRF</h2>
 *
 * <p>La autenticación viaja en un header que el cliente pone a mano, no en una
 * cookie que el navegador manda sola. Sin cookie no hay CSRF que prevenir:
 * dejarlo activado sólo rompería los POST sin proteger nada.
 *
 * <h2>Sin CORS</h2>
 *
 * <p>No hace falta y por eso no está. En desarrollo el proxy de Vite sirve
 * {@code /api} desde el mismo origen, y en producción el rewrite de Vercel hace
 * lo mismo. Abrir CORS acá sería agregar superficie para un problema que la
 * arquitectura ya evita.
 */
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    /**
     * Las rutas que se pueden pedir sin token.
     *
     * <p>Sólo el login. No hay registro público: los usuarios los crea un
     * administrador, porque quién entra a ver cuánto cobra la gente no es algo
     * que se resuelva con un formulario abierto.
     */
    private static final String[] PUBLICAS = {
        "/api/auth/login",
    };

    private final FiltroDeAutenticacionJwt filtro;
    private final ObjectMapper json;

    public SeguridadConfig(FiltroDeAutenticacionJwt filtro, ObjectMapper json) {
        this.filtro = filtro;
        this.json = json;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt con el costo por defecto (10). Sube solo con el hardware el día
        // que se cambie el número, porque el costo va escrito en cada hash.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain cadena(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(PUBLICAS).permitAll()

                        // Cargar los datos del mes es de administrador. El pliego
                        // define cuánto cobra cada uno y el padrón dice quién es
                        // quién: no son cosas que se suban desde cualquier cuenta.
                        //
                        // Que la importación también lo sea es discutible —el
                        // archivo semanal lo sube quien lo recibe, que podría ser
                        // un supervisor— y está anotado como decisión a revisar.
                        .requestMatchers(HttpMethod.POST, "/api/pliegos/**").hasRole(Rol.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/padrones/**").hasRole(Rol.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/mediciones/**").hasRole(Rol.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/perfiles/**").hasRole(Rol.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/importaciones/**").hasRole(Rol.ADMIN.name())
                        .requestMatchers("/api/usuarios/**").hasRole(Rol.ADMIN.name())

                        // El resto es lectura y necesita token. De quién es cada
                        // fila lo decide ControlDeAcceso, adentro del servicio.
                        .anyRequest().authenticated())

                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> responder(res, HttpStatus.UNAUTHORIZED,
                                "Hace falta iniciar sesión para ver esto.",
                                "Mandá el token en la cabecera: Authorization: Bearer <token>."))
                        .accessDeniedHandler((req, res, ex) -> responder(res, HttpStatus.FORBIDDEN,
                                "No tenés permiso para ver esto.",
                                "Tu usuario está autenticado, pero este recurso es de otra persona o de otro equipo.")))

                .addFilterBefore(filtro, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * El 401 y el 403 salen con el mismo formato que el resto de la API.
     *
     * <p>Spring Security devuelve por defecto un cuerpo vacío o su propio HTML, y
     * ahí el cliente tiene dos formas de error para parsear. El frontend ya sabe
     * leer {@link ErrorResponse}: que estos dos hablen el mismo idioma es lo que
     * hace que el cartel de error salga bien sin código nuevo.
     */
    private void responder(HttpServletResponse res, HttpStatus estado, String mensaje, String detalle)
            throws java.io.IOException {

        res.setStatus(estado.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getOutputStream(), ErrorResponse.de(estado, mensaje, List.of(detalle)));
    }
}
