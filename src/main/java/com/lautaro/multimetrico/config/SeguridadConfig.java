package com.lautaro.multimetrico.config;

import tools.jackson.databind.ObjectMapper;
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

@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    private static final String[] PUBLICAS = {
        "/api/auth/login",
        "/actuator/health",
        "/actuator/health/**",
    };

    private final FiltroDeAutenticacionJwt filtro;
    private final ObjectMapper json;

    public SeguridadConfig(FiltroDeAutenticacionJwt filtro, ObjectMapper json) {
        this.filtro = filtro;
        this.json = json;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain cadena(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(rutas -> rutas
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()

                        .requestMatchers(PUBLICAS).permitAll()

                        .requestMatchers("/api/usuarios/**").hasRole(Rol.ADMIN.name())

                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()

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

    private void responder(HttpServletResponse res, HttpStatus estado, String mensaje, String detalle)
            throws java.io.IOException {

        res.setStatus(estado.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getOutputStream(), ErrorResponse.de(estado, mensaje, List.of(detalle)));
    }
}
