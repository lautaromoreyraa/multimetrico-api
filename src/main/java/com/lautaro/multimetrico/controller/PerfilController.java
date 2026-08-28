package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.dto.perfil.InspeccionResponse;
import com.lautaro.multimetrico.dto.perfil.PerfilRequest;
import com.lautaro.multimetrico.dto.perfil.PerfilResponse;
import com.lautaro.multimetrico.mapper.PerfilMapper;
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

/**
 * Los perfiles de importación: dónde está cada cosa dentro de una planilla de
 * origen.
 *
 * <p>Está separado de {@link ImportacionController} aunque los dos usen el mismo
 * servicio, porque son dos recursos distintos y dos momentos distintos: el perfil
 * se define una vez cuando aparece una fuente nueva, y las importaciones se
 * suben todas las semanas.
 */
@RestController
@RequestMapping("/api/perfiles")
public class PerfilController {

    private final ImportacionService servicio;
    private final PerfilMapper mapper;

    public PerfilController(ImportacionService servicio, PerfilMapper mapper) {
        this.servicio = servicio;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<PerfilResponse> definir(@Valid @RequestBody PerfilRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.aResponse(servicio.definirPerfil(mapper.aEntidad(request))));
    }

    @GetMapping
    public List<PerfilResponse> perfiles() {
        return mapper.aResponses(servicio.perfiles());
    }

    /**
     * Devuelve las columnas del archivo con valores de ejemplo.
     *
     * <p>Es el arranque cuando llega una fuente nueva: el sistema no adivina qué
     * columna es cuál, muestra lo que hay y una persona arma el perfil mirándolo.
     */
    @PostMapping("/inspeccion")
    public InspeccionResponse inspeccionar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(required = false) String hoja,
            @RequestParam(defaultValue = "1") int filaEncabezados) throws IOException {

        byte[] bytes = archivo.getBytes();
        return mapper.aInspeccion(
                servicio.hojasDe(bytes),
                filaEncabezados,
                servicio.inspeccionar(bytes, hoja, filaEncabezados));
    }
}
