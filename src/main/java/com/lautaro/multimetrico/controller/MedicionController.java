package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.dto.medicion.AltaResponse;
import com.lautaro.multimetrico.dto.medicion.HistoriaResponse;
import com.lautaro.multimetrico.dto.medicion.MedicionesRequest;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.mapper.MedicionMapper;
import com.lautaro.multimetrico.service.medicion.MedicionService;
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
@RequestMapping("/api/mediciones")
public class MedicionController {

    private final MedicionService servicio;
    private final MedicionMapper mapper;

    public MedicionController(MedicionService servicio, MedicionMapper mapper) {
        this.servicio = servicio;
        this.mapper = mapper;
    }

    /**
     * Alta en lote. Siempre agrega: no existe un PUT que pise lo cargado.
     *
     * <p>Es un 201 y no un 200 porque cada carga crea mediciones nuevas, incluso
     * cuando repite asesores y métricas que ya estaban.
     */
    @PostMapping
    public ResponseEntity<AltaResponse> registrar(@Valid @RequestBody MedicionesRequest request) {
        List<Medicion> registradas = servicio.registrar(mapper.aEntidades(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.aAlta(request.periodo(), registradas));
    }

    /**
     * La historia del mes, opcionalmente de un asesor y de una métrica.
     *
     * <p>Sin filtros trae la operación entera. Los dos filtros juntos —que es el
     * uso real— dan la serie de una métrica de una persona dentro del mes, con
     * la vigente marcada.
     */
    @GetMapping("/{periodo}")
    public HistoriaResponse historia(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam(required = false) String legajo,
            @RequestParam(required = false) Metrica metrica) {

        List<Medicion> historia = servicio.historia(periodo, legajo, metrica);
        return mapper.aHistoria(periodo, legajo, metrica, historia, servicio.vigentesEn(historia));
    }
}
