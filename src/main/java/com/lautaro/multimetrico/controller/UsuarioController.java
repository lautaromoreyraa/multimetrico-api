package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.dto.seguridad.UsuarioRequest;
import com.lautaro.multimetrico.dto.seguridad.UsuarioResponse;
import com.lautaro.multimetrico.service.seguridad.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarios.crear(request));
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarios.listar();
    }
}
