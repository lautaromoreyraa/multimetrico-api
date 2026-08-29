package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.dto.padron.PadronCargadoResponse;
import com.lautaro.multimetrico.dto.padron.PadronRequest;
import com.lautaro.multimetrico.dto.resolucion.AliasConfirmadoResponse;
import com.lautaro.multimetrico.dto.resolucion.AliasRequest;
import com.lautaro.multimetrico.dto.resolucion.ResolucionRequest;
import com.lautaro.multimetrico.dto.resolucion.ResolucionResponse;
import com.lautaro.multimetrico.mapper.PadronMapper;
import com.lautaro.multimetrico.mapper.ResolucionMapper;
import com.lautaro.multimetrico.service.padron.PadronService;
import com.lautaro.multimetrico.util.PeriodoUtil;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.lautaro.multimetrico.dto.padron.AsesorRequest;
import com.lautaro.multimetrico.dto.padron.CorreccionDeAsesorRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final PadronMapper mapper;
    private final ResolucionMapper resolucionMapper;

    public PadronController(PadronService servicio, PadronMapper mapper, ResolucionMapper resolucionMapper) {
        this.servicio = servicio;
        this.mapper = mapper;
        this.resolucionMapper = resolucionMapper;
    }

    @PostMapping
    public ResponseEntity<PadronCargadoResponse> cargar(@Valid @RequestBody PadronRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.aCargado(servicio.cargar(mapper.aEntidad(request))));
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
    public List<AsesorResponse> ver(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam(required = false) String lider) {

        List<Asesor> asesores = lider == null
                ? servicio.buscar(periodo).asesores()
                : servicio.equipo(periodo, lider);

        return mapper.aResponses(asesores);
    }

    @GetMapping("/{periodo}/lideres")
    public List<String> lideres(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo) {
        return servicio.lideres(periodo);
    }

    /**
     * Cruza los nombres de un archivo contra el padrón sin importar nada.
     *
     * <p>Pensado para correr antes de subir: deja ver qué filas van a empalmar y
     * cuáles no, mientras todavía se puede arreglar.
     */
    /**
     * Suma una persona a la nómina del mes.
     *
     * <p>Existe para corregir de a uno: el padrón se carga entero desde un
     * archivo, y agregar a alguien que faltaba no puede obligar a volver a subir
     * las cincuenta filas.
     */
    @PostMapping("/{periodo}/asesores")
    public ResponseEntity<AsesorResponse> agregar(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @Valid @RequestBody AsesorRequest request) {

        Asesor agregado = servicio.agregarAsesor(periodo,
                new Asesor(request.legajo(), request.nombre(), request.lider()));

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.aResponse(agregado));
    }

    /**
     * Corrige el nombre o el equipo de alguien.
     *
     * <p>El legajo va en la URL y no en el cuerpo porque <b>no se puede
     * cambiar</b>: es la identidad con la que se guardaron sus mediciones e
     * incentivos. Ver {@code Asesor.corregir}.
     */
    @PutMapping("/{periodo}/asesores/{legajo}")
    public AsesorResponse corregir(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @PathVariable String legajo,
            @Valid @RequestBody CorreccionDeAsesorRequest request) {

        return mapper.aResponse(
                servicio.corregirAsesor(periodo, legajo, request.nombre(), request.lider()));
    }

    /**
     * Saca a una persona de la nómina del mes.
     *
     * <p>Deja de aparecer en el multimétrico y en los incentivos. Sus mediciones e
     * importes quedan guardados: si vuelve a cargarse, vuelven con ella.
     *
     * <p>Es un DELETE, así que pide cuenta de administrador — lo decide
     * {@code SeguridadConfig} por el verbo, no una anotación acá.
     */
    @DeleteMapping("/{periodo}/asesores/{legajo}")
    public AsesorResponse quitar(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @PathVariable String legajo) {
        return mapper.aResponse(servicio.quitarAsesor(periodo, legajo));
    }

    @PostMapping("/{periodo}/resolucion")
    public ResolucionResponse resolver(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @Valid @RequestBody ResolucionRequest request) {
        return resolucionMapper.aResponse(servicio.resolver(periodo, request.nombres()));
    }

    /** Una persona confirma que un texto es tal legajo; el sistema no vuelve a preguntar. */
    @PostMapping("/{periodo}/alias")
    public AliasConfirmadoResponse confirmarAlias(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @Valid @RequestBody AliasRequest request) {

        Asesor asesor = servicio.confirmarAlias(periodo, request.texto(), request.legajo());
        return new AliasConfirmadoResponse(
                request.texto(),
                asesor.legajo(),
                asesor.nombre(),
                servicio.aliasDe(asesor.legajo()));
    }
}
