package com.lautaro.multimetrico.service.seguridad;

import com.lautaro.multimetrico.dto.seguridad.LoginRequest;
import com.lautaro.multimetrico.dto.seguridad.TokenResponse;
import com.lautaro.multimetrico.dto.seguridad.UsuarioRequest;
import com.lautaro.multimetrico.dto.seguridad.UsuarioResponse;
import java.util.List;

public interface UsuarioService {

    TokenResponse entrar(LoginRequest request);

    UsuarioResponse crear(UsuarioRequest request);

    List<UsuarioResponse> listar();
}
