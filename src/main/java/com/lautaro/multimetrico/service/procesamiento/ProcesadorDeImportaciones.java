package com.lautaro.multimetrico.service.procesamiento;

import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.importacion.Informe;
import com.lautaro.multimetrico.dto.planilla.FilaLeida;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.BiFunction;

public interface ProcesadorDeImportaciones {

    String POOL = "importaciones";

    void procesar(String id, PerfilDeImportacion perfil, YearMonth periodo, LocalDate corte,
            byte[] archivo, BiFunction<String, List<FilaLeida>, Informe> interprete);
}
