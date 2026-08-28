package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.dto.importacion.ImportacionResponse;
import com.lautaro.multimetrico.mapper.ImportacionMapper;
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
    private final ImportacionMapper mapper;

    public ImportacionController(ImportacionService servicio, ImportacionMapper mapper) {
        this.servicio = servicio;
        this.mapper = mapper;
    }

    /**
     * Recibe el archivo y responde con el id.
     *
     * <p>Devuelve {@code 202 Accepted} y no {@code 201}: cuando la respuesta sale,
     * la importación todavía se está procesando. El estado se consulta después.
     */
    @PostMapping
    public ResponseEntity<ImportacionResponse> importar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam String perfil,
            @RequestParam @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate corte) throws IOException {

        var recibida = servicio.recibir(perfil, periodo, corte, archivo.getBytes());
        return ResponseEntity.accepted().body(mapper.aResponse(recibida));
    }

    @GetMapping("/{id}")
    public ImportacionResponse ver(@PathVariable String id) {
        return mapper.aResponse(servicio.buscar(id));
    }

    @GetMapping
    public List<ImportacionResponse> historial() {
        return servicio.historial().stream().map(mapper::aResponse).toList();
    }

    /**
     * Asigna a mano una fila que quedó pendiente.
     *
     * <p>Vale para esta fila de este archivo, y no crea un alias global: un nombre
     * cortado que apunta a dos personas puede ser una este mes y la otra el
     * siguiente.
     */
    @PostMapping("/{id}/pendientes/{fila}")
    public ImportacionResponse resolver(
            @PathVariable String id,
            @PathVariable int fila,
            @RequestParam String legajo) {
        return mapper.aResponse(servicio.resolverPendiente(id, fila, legajo));
    }
}
