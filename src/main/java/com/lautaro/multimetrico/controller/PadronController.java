package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.dto.padron.AsesorRequest;
import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.dto.padron.CorreccionDeAsesorRequest;
import com.lautaro.multimetrico.dto.padron.PadronCargadoResponse;
import com.lautaro.multimetrico.dto.padron.PadronRequest;
import com.lautaro.multimetrico.dto.resolucion.AliasConfirmadoResponse;
import com.lautaro.multimetrico.dto.resolucion.AliasRequest;
import com.lautaro.multimetrico.dto.resolucion.ResolucionRequest;
import com.lautaro.multimetrico.dto.resolucion.ResolucionResponse;
import com.lautaro.multimetrico.service.padron.PadronService;
import com.lautaro.multimetrico.util.PeriodoUtil;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/padrones")
public class PadronController {

    private final PadronService servicio;

    public PadronController(PadronService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<PadronCargadoResponse> cargar(@Valid @RequestBody PadronRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.cargar(request));
    }

    @GetMapping
    public List<YearMonth> periodos() {
        return servicio.periodos();
    }

    @GetMapping("/{periodo}")
    public List<AsesorResponse> ver(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam(required = false) String lider) {
        return servicio.asesores(periodo, lider);
    }

    @GetMapping("/{periodo}/lideres")
    public List<String> lideres(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo) {
        return servicio.lideres(periodo);
    }

    @PostMapping("/{periodo}/asesores")
    public ResponseEntity<AsesorResponse> agregar(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @Valid @RequestBody AsesorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.agregarAsesor(periodo, request));
    }

    @PutMapping("/{periodo}/asesores/{legajo}")
    public AsesorResponse corregir(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @PathVariable String legajo,
            @Valid @RequestBody CorreccionDeAsesorRequest request) {
        return servicio.corregirAsesor(periodo, legajo, request);
    }

    @DeleteMapping("/{periodo}/asesores/{legajo}")
    public AsesorResponse quitar(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @PathVariable String legajo) {
        return servicio.quitarDelPadron(periodo, legajo);
    }

    @PostMapping("/{periodo}/resolucion")
    public ResolucionResponse resolver(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @Valid @RequestBody ResolucionRequest request) {
        return servicio.resolver(periodo, request);
    }

    @PostMapping("/{periodo}/alias")
    public AliasConfirmadoResponse confirmarAlias(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @Valid @RequestBody AliasRequest request) {
        return servicio.confirmarAlias(periodo, request);
    }
}
