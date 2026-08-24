package com.lautaro.multimetrico.multimetrico.web;

import com.lautaro.multimetrico.multimetrico.MultimetricoService;
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

    public MultimetricoController(MultimetricoService servicio) {
        this.servicio = servicio;
    }

    /**
     * El multimétrico del equipo de un líder; sin {@code lider}, la operación
     * entera.
     *
     * <p>El período va en la URL y la campaña es obligatoria, igual que en la
     * evaluación del pliego: no existe "el multimétrico de ahora". Falta el
     * pliego o el padrón de ese período y responde 404 diciendo cuál de los dos.
     */
    @GetMapping("/{periodo}")
    public MultimetricoResponse delEquipo(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo,
            @RequestParam(required = false) String lider,
            @RequestParam String campania) {
        return MultimetricoResponse.de(servicio.delEquipo(periodo, lider, campania));
    }

    /** La fila de una persona: la pantalla "mi multimétrico". */
    @GetMapping("/{periodo}/asesores/{legajo}")
    public FilaResponse deUnAsesor(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo,
            @PathVariable String legajo,
            @RequestParam String campania) {
        return FilaResponse.de(servicio.deUnAsesor(periodo, legajo, campania));
    }
}
