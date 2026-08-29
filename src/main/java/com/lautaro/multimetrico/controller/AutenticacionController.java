package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.dto.seguridad.Credencial;
import com.lautaro.multimetrico.dto.seguridad.LoginRequest;
import com.lautaro.multimetrico.dto.seguridad.TokenResponse;
import com.lautaro.multimetrico.exception.AccesoDenegadoException;
import com.lautaro.multimetrico.service.seguridad.ControlDeAcceso;
import com.lautaro.multimetrico.service.seguridad.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Entrar, y preguntar quién soy. */
@RestController
@RequestMapping("/api/auth")
public class AutenticacionController {

    private final UsuarioService usuarios;
    private final ControlDeAcceso acceso;

    public AutenticacionController(UsuarioService usuarios, ControlDeAcceso acceso) {
        this.usuarios = usuarios;
        this.acceso = acceso;
    }

    /**
     * El único endpoint público de la API.
     *
     * <p>Responde 401 con el mismo mensaje para cualquier motivo de fallo. Ver
     * {@code UsuarioServiceImpl}.
     */
    @PostMapping("/login")
    public TokenResponse entrar(@Valid @RequestBody LoginRequest request) {
        return usuarios.entrar(request);
    }

    /**
     * Quién es el dueño del token que vino.
     *
     * <p>Existe para que la pantalla pueda recuperar la sesión en un F5 sin
     * guardar el rol por su cuenta, y para comprobar de un vistazo si un token
     * sigue vivo.
     */
    @GetMapping("/yo")
    public Credencial yo() {
        return acceso.quienPide().orElseThrow(
                () -> new AccesoDenegadoException("No hay ningún usuario autenticado."));
    }
}
