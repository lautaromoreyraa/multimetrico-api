package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.dto.multimetrico.FilaResponse;
import com.lautaro.multimetrico.dto.multimetrico.MultimetricoResponse;
import com.lautaro.multimetrico.mapper.MultimetricoMapper;
import com.lautaro.multimetrico.service.multimetrico.MultimetricoService;
import com.lautaro.multimetrico.util.PeriodoUtil;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/multimetrico")
public class MultimetricoController {

    private final MultimetricoService servicio;
    private final MultimetricoMapper mapper;

    public MultimetricoController(MultimetricoService servicio, MultimetricoMapper mapper) {
        this.servicio = servicio;
        this.mapper = mapper;
    }

    @GetMapping("/{periodo}")
    public MultimetricoResponse delEquipo(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam(required = false) String lider,
            @RequestParam String campania) {
        return mapper.aResponse(servicio.delEquipo(periodo, lider, campania));
    }

    @GetMapping("/{periodo}/asesores/{legajo}")
    public FilaResponse deUnAsesor(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @PathVariable String legajo,
            @RequestParam String campania) {
        return mapper.aResponse(servicio.deUnAsesor(periodo, legajo, campania));
    }
}
