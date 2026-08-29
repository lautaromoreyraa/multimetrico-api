package com.lautaro.multimetrico.controller;

import com.lautaro.multimetrico.dto.incentivo.CargaDeIncentivos;
import com.lautaro.multimetrico.dto.incentivo.IncentivosResponse;
import com.lautaro.multimetrico.dto.perfil.PerfilDeIncentivos;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;
import com.lautaro.multimetrico.service.incentivo.IncentivoService;
import com.lautaro.multimetrico.util.PeriodoUtil;
import java.io.IOException;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Los importes del mes.
 *
 * <p>Leer es público y cargar es de administrador, igual que todo lo demás: la
 * regla está en el verbo. Lo decide {@code SeguridadConfig} para toda la API, no
 * una anotación acá.
 */
@RestController
@RequestMapping("/api/incentivos")
public class IncentivoController {

    private final IncentivoService servicio;

    public IncentivoController(IncentivoService servicio) {
        this.servicio = servicio;
    }

    /**
     * Sube la planilla de incentivos del mes.
     *
     * <p>Devuelve qué pasó con cada fila. Es sincrónico y no diferido como la
     * importación de métricas: aquel archivo trae doscientas filas por semana y
     * éste una por persona y por mes, así que esperar la respuesta no molesta y
     * el informe llega en el mismo request.
     */
    @PostMapping("/{periodo}")
    public CargaDeIncentivos cargar(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(required = false) String hoja,
            @RequestParam(required = false) Integer filaEncabezados,
            @RequestParam String columnaAsesor,
            @RequestParam String columnaImporte,
            @RequestParam(required = false) SeparadorDecimal separadorDecimal) throws IOException {

        var perfil = new PerfilDeIncentivos(
                hoja, filaEncabezados, columnaAsesor, columnaImporte, separadorDecimal);

        return servicio.cargar(periodo, perfil, archivo.getBytes(), archivo.getOriginalFilename());
    }

    /**
     * Los incentivos de un equipo; sin {@code lider}, la operación entera.
     *
     * <p>Muestra la lista completa de asesores del padrón, igual que el
     * multimétrico: quien no vino en la planilla aparece con el importe vacío en
     * vez de desaparecer.
     */
    @GetMapping("/{periodo}")
    public IncentivosResponse delEquipo(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam(required = false) String lider) {
        return servicio.delEquipo(periodo, lider);
    }
}
