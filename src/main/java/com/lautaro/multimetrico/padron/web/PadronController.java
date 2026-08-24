package com.lautaro.multimetrico.padron.web;

import com.lautaro.multimetrico.padron.PadronService;
import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.padron.domain.Padron;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/padrones")
public class PadronController {

    private final PadronService servicio;

    public PadronController(PadronService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> cargar(@Valid @RequestBody PadronRequest request) {
        List<Asesor> asesores = request.asesores().stream()
                .map(a -> new Asesor(a.legajo(), a.nombre(), a.lider()))
                .toList();

        Padron padron = servicio.cargar(new Padron(request.periodo(), asesores));

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "periodo", padron.periodo().toString(),
                "asesores", padron.tamano(),
                "lideres", padron.lideres()));
    }

    @GetMapping
    public List<YearMonth> periodos() {
        return servicio.periodos();
    }

    /**
     * El equipo de un líder. Sin el parámetro devuelve la operación entera, que
     * es como viene el archivo de origen.
     */
    @GetMapping("/{periodo}")
    public List<Asesor> ver(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo,
            @RequestParam(required = false) String lider) {
        return lider == null ? servicio.buscar(periodo).asesores() : servicio.equipo(periodo, lider);
    }

    @GetMapping("/{periodo}/lideres")
    public List<String> lideres(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo) {
        return servicio.lideres(periodo);
    }

    /**
     * Cruza los nombres de un archivo contra el padrón sin importar nada.
     *
     * <p>Pensado para correr antes de subir: deja ver qué filas van a empalmar y
     * cuáles no, mientras todavía se puede arreglar.
     */
    @PostMapping("/{periodo}/resolucion")
    public ResolucionResponse resolver(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo,
            @Valid @RequestBody ResolucionRequest request) {
        return ResolucionResponse.de(servicio.resolver(periodo, request.nombres()));
    }

    /** Una persona confirma que un texto es tal legajo; el sistema no vuelve a preguntar. */
    @PostMapping("/{periodo}/alias")
    public Map<String, Object> confirmarAlias(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo,
            @Valid @RequestBody AliasRequest request) {
        Asesor asesor = servicio.confirmarAlias(periodo, request.texto(), request.legajo());
        return Map.of(
                "texto", request.texto(),
                "legajo", asesor.legajo(),
                "nombre", asesor.nombre(),
                "aliasDeEsteLegajo", servicio.aliasDe(asesor.legajo()));
    }
}
