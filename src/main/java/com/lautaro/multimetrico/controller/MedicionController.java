package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.dto.medicion.AltaResponse;
import com.lautaro.multimetrico.dto.medicion.HistoriaResponse;
import com.lautaro.multimetrico.dto.medicion.MedicionesRequest;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.service.medicion.MedicionService;
import com.lautaro.multimetrico.util.PeriodoUtil;
import jakarta.validation.Valid;
import java.time.YearMonth;
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
@RequestMapping("/api/mediciones")
public class MedicionController {

    private final MedicionService servicio;

    public MedicionController(MedicionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<AltaResponse> registrar(@Valid @RequestBody MedicionesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(request));
    }

    @GetMapping("/{periodo}")
    public HistoriaResponse historia(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam(required = false) String legajo,
            @RequestParam(required = false) Metrica metrica) {

        return servicio.historiaDelPeriodo(periodo, legajo, metrica);
    }
}
