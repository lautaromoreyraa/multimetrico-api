package com.lautaro.multimetrico.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lautaro.multimetrico.dto.error.ErrorResponse;
import com.lautaro.multimetrico.enumeration.Rol;
import jakarta.servlet.DispatcherType;
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
 * <h2>Leer es público, escribir necesita entrar</h2>
 *
 * <p>La línea está en el verbo, no en el recurso: <b>todos los GET son
 * públicos</b> y todo lo que modifica datos pide una cuenta de administrador.
 *
 * <p>Es una decisión de producto y conviene decir qué se resigna. El PLAN §5.6
 * pedía control <i>por fila</i> —que un asesor viera sólo la suya— y eso ya no
 * está: con la lectura abierta, cualquiera que tenga la URL ve la tabla entera.
 * A cambio, la pantalla se puede mostrar sin pedirle nada a nadie, que es lo que
 * hace falta hoy.
 *
 * <p>Lo que sostiene esa decisión es que <b>los datos son sintéticos</b>. El día
 * que entren números de una operación real, esto tiene que volver a cerrarse:
 * la infraestructura de roles sigue acá y {@code Rol} ya distingue ASESOR de
 * SUPERVISOR, así que es volver a poner el control, no rehacerlo.
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
     * Lo que se puede pedir sin token, además de todos los GET.
     *
     * <p>Sólo el login. No hay registro público: los usuarios los crea un
     * administrador, porque quién carga el pliego del mes no es algo que se
     * resuelva con un formulario abierto.
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
                        // El despacho a /error no es un recurso: es Spring
                        // renderizando el error de una request que YA pasó por
                        // acá. Sin esta línea, cualquier excepción que no tenga
                        // manejador termina en un 401 que dice "iniciá sesión",
                        // incluso en un GET público y aunque el token esté bien.
                        //
                        // Pasaba de verdad: pedir /api/multimetrico/2026-08 sin
                        // el parámetro campania devolvía 401 en vez de 400.
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()

                        .requestMatchers(PUBLICAS).permitAll()

                        // El orden importa y es la parte fácil de romper: Spring
                        // evalúa de arriba abajo y se queda con la primera regla
                        // que coincide. Los usuarios van ANTES del permitAll de
                        // los GET, porque si no el GET los alcanza primero y los
                        // deja abiertos. Hay un test que lo afirma.
                        //
                        // Son la excepción a "leer es público": listarlos es leer,
                        // pero es leer quién tiene acceso.
                        .requestMatchers("/api/usuarios/**").hasRole(Rol.ADMIN.name())

                        // Leer es público: la tabla del equipo, la fila de una
                        // persona, el pliego del mes y los informes de las
                        // importaciones se muestran sin pedir nada.
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()

                        // Todo lo que escribe es de administrador. El pliego
                        // define cuánto cobra cada uno y el padrón dice quién es
                        // quién: no son cosas que se suban desde cualquier lado.
                        //
                        // La regla va por verbo y no por ruta a propósito: así el
                        // endpoint de escritura que se agregue mañana queda
                        // protegido sin que nadie se acuerde de agregarlo acá.
                        .anyRequest().hasRole(Rol.ADMIN.name()))

                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> responder(res, HttpStatus.UNAUTHORIZED,
                                "Hace falta iniciar sesión para hacer esto.",
                                "Entrá al panel de administración. Leer no pide sesión; cargar datos, sí."))
                        .accessDeniedHandler((req, res, ex) -> responder(res, HttpStatus.FORBIDDEN,
                                "Tu usuario no puede hacer esto.",
                                "Cargar datos es de administrador.")))

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
