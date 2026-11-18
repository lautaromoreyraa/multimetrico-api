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

@RestController
@RequestMapping("/api/incentivos")
public class IncentivoController {

    private final IncentivoService servicio;

    public IncentivoController(IncentivoService servicio) {
        this.servicio = servicio;
    }

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

    @GetMapping("/{periodo}")
    public IncentivosResponse delEquipo(
            @PathVariable @DateTimeFormat(pattern = PeriodoUtil.PATRON) YearMonth periodo,
            @RequestParam(required = false) String lider) {
        return servicio.delEquipo(periodo, lider);
    }
}
