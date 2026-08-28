package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.evaluacion.EvaluacionRequest;
import com.lautaro.multimetrico.dto.evaluacion.EvaluacionResponse;
import com.lautaro.multimetrico.dto.pliego.PliegoRequest;
import com.lautaro.multimetrico.dto.pliego.PliegoResponse;
import com.lautaro.multimetrico.dto.pliego.Violacion;
import com.lautaro.multimetrico.mapper.PliegoMapper;
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
    private final PliegoMapper mapper;

    public PliegoController(PliegoService servicio, PliegoMapper mapper) {
        this.servicio = servicio;
        this.mapper = mapper;
    }

    /**
     * Revisa un pliego sin guardarlo. Devuelve 200 con la lista de problemas
     * —vacía si está sano—, porque acá "tiene errores" es la respuesta esperada
     * y no una falla de la petición.
     */
    @PostMapping("/revision")
    public List<Violacion> revisar(@Valid @RequestBody PliegoRequest request) {
        return servicio.revisar(mapper.aEntidad(request));
    }

    @PostMapping
    public ResponseEntity<PliegoResponse> cargar(@Valid @RequestBody PliegoRequest request) {
        Pliego cargado = servicio.cargar(mapper.aEntidad(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.aResponse(cargado));
    }

    @GetMapping
    public List<PliegoResponse> listar() {
        return mapper.aResponses(servicio.listar());
    }

    @GetMapping("/{periodo}")
    public PliegoResponse buscar(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam String campania) {
        return mapper.aResponse(servicio.buscar(periodo, campania));
    }

    /**
     * Evalúa mediciones contra el pliego de ese período.
     *
     * <p>El período va en la URL y no se toma "el vigente": un cálculo tiene que
     * poder repetirse dentro de seis meses y dar lo mismo.
     */
    @PostMapping("/{periodo}/evaluacion")
    public EvaluacionResponse evaluar(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam String campania,
            @Valid @RequestBody EvaluacionRequest request) {
        return mapper.aResponse(servicio.evaluar(periodo, campania, request.mediciones()));
    }
}
