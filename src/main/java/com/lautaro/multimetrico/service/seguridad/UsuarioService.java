package com.lautaro.multimetrico.service.seguridad;

import com.lautaro.multimetrico.domain.Usuario;
import com.lautaro.multimetrico.dto.seguridad.LoginRequest;
import com.lautaro.multimetrico.dto.seguridad.TokenResponse;
import com.lautaro.multimetrico.dto.seguridad.UsuarioRequest;
import java.util.List;

public interface UsuarioService {

    TokenResponse entrar(LoginRequest request);

    Usuario crear(UsuarioRequest request);

    List<Usuario> listar();
}
