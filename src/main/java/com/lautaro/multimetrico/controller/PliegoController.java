package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.dto.evaluacion.EvaluacionRequest;
import com.lautaro.multimetrico.dto.evaluacion.EvaluacionResponse;
import com.lautaro.multimetrico.dto.pliego.PliegoRequest;
import com.lautaro.multimetrico.dto.pliego.PliegoResponse;
import com.lautaro.multimetrico.dto.pliego.Violacion;
import com.lautaro.multimetrico.service.pliego.PliegoService;
import com.lautaro.multimetrico.util.PeriodoUtil;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pliegos")
public class PliegoController {

    private final PliegoService servicio;

    public PliegoController(PliegoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/revision")
    public List<Violacion> revisar(@Valid @RequestBody PliegoRequest request) {
        return servicio.revisar(request);
    }

    @PostMapping
    public ResponseEntity<PliegoResponse> cargar(@Valid @RequestBody PliegoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.cargar(request));
    }

    @GetMapping
    public List<PliegoResponse> listar() {
        return servicio.pliegosCargados();
    }

    @GetMapping("/{periodo}")
    public PliegoResponse buscar(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam String campania) {
        return servicio.verPliego(periodo, campania);
    }

    @PostMapping("/{periodo}/evaluacion")
    public EvaluacionResponse evaluar(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam String campania,
            @Valid @RequestBody EvaluacionRequest request) {
        return servicio.evaluar(periodo, campania, request);
    }
}
