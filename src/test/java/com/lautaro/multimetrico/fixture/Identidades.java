package com.lautaro.multimetrico.fixture;

import com.lautaro.multimetrico.dto.seguridad.Credencial;
import com.lautaro.multimetrico.enumeration.Rol;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class Identidades {

    private Identidades() {
    }

    public static Credencial admin() {
        return new Credencial("admin", Rol.ADMIN, null);
    }

    public static Credencial supervisor(String lider) {
        return new Credencial("sup-" + lider, Rol.SUPERVISOR, lider);
    }

    public static Credencial asesor(String legajo) {
        return new Credencial(legajo, Rol.ASESOR, null);
    }

    public static RequestPostProcessor como(Credencial credencial) {
        return SecurityMockMvcRequestPostProcessors.authentication(autenticacion(credencial));
    }

    public static void actuarComo(Credencial credencial) {
        SecurityContextHolder.getContext().setAuthentication(autenticacion(credencial));
    }

    public static void olvidar() {
        SecurityContextHolder.clearContext();
    }

    private static UsernamePasswordAuthenticationToken autenticacion(Credencial credencial) {
        return new UsernamePasswordAuthenticationToken(
                credencial,
                null,
                List.of(new SimpleGrantedAuthority(credencial.rol().autoridad())));
    }
}
