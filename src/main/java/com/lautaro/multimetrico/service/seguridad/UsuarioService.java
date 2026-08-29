package com.lautaro.multimetrico.service.seguridad;

import com.lautaro.multimetrico.domain.Usuario;
import com.lautaro.multimetrico.dto.seguridad.LoginRequest;
import com.lautaro.multimetrico.dto.seguridad.TokenResponse;
import com.lautaro.multimetrico.dto.seguridad.UsuarioRequest;
import java.util.List;

/** Alta de usuarios y entrada al sistema. */
public interface UsuarioService {

    /**
     * Valida las credenciales y devuelve el token.
     *
     * <p>Falla con el <b>mismo</b> mensaje cuando el legajo no existe y cuando la
     * contraseña está mal. Decir cuál de las dos falló convierte al login en una
     * forma de averiguar qué legajos existen.
     */
    TokenResponse entrar(LoginRequest request);

    Usuario crear(UsuarioRequest request);

    List<Usuario> listar();
}
