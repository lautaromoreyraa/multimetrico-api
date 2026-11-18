package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.dto.importacion.ImportacionResponse;
import com.lautaro.multimetrico.service.importacion.ImportacionService;
import com.lautaro.multimetrico.util.PeriodoUtil;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/importaciones")
public class ImportacionController {

    private final ImportacionService servicio;

    public ImportacionController(ImportacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ImportacionResponse> importar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam String perfil,
            @RequestParam @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate corte) throws IOException {

        return ResponseEntity.accepted().body(servicio.importar(perfil, periodo, corte, archivo.getBytes()));
    }

    @GetMapping("/{id}")
    public ImportacionResponse ver(@PathVariable String id) {
        return servicio.ver(id);
    }

    @GetMapping
    public List<ImportacionResponse> historial() {
        return servicio.importaciones();
    }

    @PostMapping("/{id}/pendientes/{fila}")
    public ImportacionResponse resolver(
            @PathVariable String id,
            @PathVariable int fila,
            @RequestParam String legajo) {
        return servicio.asignarPendiente(id, fila, legajo);
    }
}
