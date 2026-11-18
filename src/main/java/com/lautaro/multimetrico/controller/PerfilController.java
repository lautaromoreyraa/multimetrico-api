package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.dto.perfil.InspeccionResponse;
import com.lautaro.multimetrico.dto.perfil.PerfilRequest;
import com.lautaro.multimetrico.dto.perfil.PerfilResponse;
import com.lautaro.multimetrico.service.importacion.ImportacionService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/perfiles")
public class PerfilController {

    private final ImportacionService servicio;

    public PerfilController(ImportacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<PerfilResponse> definir(@Valid @RequestBody PerfilRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.definirPerfil(request));
    }

    @GetMapping
    public List<PerfilResponse> perfiles() {
        return servicio.perfilesDefinidos();
    }

    @PostMapping("/inspeccion")
    public InspeccionResponse inspeccionar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(required = false) String hoja,
            @RequestParam(defaultValue = "1") int filaEncabezados) throws IOException {

        return servicio.inspeccionarPlanilla(archivo.getBytes(), hoja, filaEncabezados);
    }
}
