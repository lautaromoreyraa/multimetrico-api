package com.lautaro.multimetrico.fixture;

import com.lautaro.multimetrico.dto.seguridad.Credencial;
import com.lautaro.multimetrico.enumeration.Rol;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Quién hace cada request en los tests.
 *
 * <p>No se usa {@code @WithMockUser} a propósito: pone un {@code String} como
 * principal y {@code ControlDeAcceso} espera una {@link Credencial}, así que los
 * tests pasarían por el rol y fallarían en el control por fila — justo la mitad
 * que importa probar.
 *
 * <p>Acá se arma la misma autenticación que armaría el filtro al leer un token
 * válido. Los tests ejercitan entonces el mismo camino que una request real, sin
 * tener que emitir y mandar un token en cada uno.
 */
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

    /** Para MockMvc: {@code .with(como(admin()))}. */
    public static RequestPostProcessor como(Credencial credencial) {
        return SecurityMockMvcRequestPostProcessors.authentication(autenticacion(credencial));
    }

    /**
     * Para los tests que llaman al servicio directo, sin pasar por HTTP.
     *
     * <p>Hay que limpiarlo después con {@link #olvidar()}: el contexto de
     * seguridad vive en un ThreadLocal y JUnit reusa el hilo entre tests, así que
     * una identidad que queda puesta se filtra al siguiente.
     */
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
