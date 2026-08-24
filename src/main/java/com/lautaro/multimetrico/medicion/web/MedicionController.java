package com.lautaro.multimetrico.medicion.web;

import com.lautaro.multimetrico.medicion.MedicionService;
import com.lautaro.multimetrico.medicion.domain.Medicion;
import com.lautaro.multimetrico.pliego.domain.Metrica;
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

    public MedicionController(MedicionService servicio) {
        this.servicio = servicio;
    }

    /**
     * Alta en lote. Siempre agrega: no existe un PUT que pise lo cargado.
     *
     * <p>Es un 201 y no un 200 porque cada carga crea mediciones nuevas, incluso
     * cuando repite asesores y métricas que ya estaban.
     */
    @PostMapping
    public ResponseEntity<AltaResponse> registrar(@Valid @RequestBody MedicionesRequest request) {
        List<Medicion> registradas = servicio.registrar(request.aDominio());
        return ResponseEntity.status(HttpStatus.CREATED).body(AltaResponse.de(request.periodo(), registradas));
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
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo,
            @RequestParam(required = false) String legajo,
            @RequestParam(required = false) Metrica metrica) {
        return HistoriaResponse.de(periodo, legajo, metrica, servicio.historia(periodo, legajo, metrica));
    }
}
