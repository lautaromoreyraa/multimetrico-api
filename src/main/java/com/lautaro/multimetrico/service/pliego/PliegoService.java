package com.lautaro.multimetrico.service.pliego;

import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.evaluacion.Evaluacion;
import com.lautaro.multimetrico.dto.pliego.Violacion;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public interface PliegoService {

    List<Violacion> revisar(Pliego pliego);

    Pliego cargar(Pliego pliego);

    Pliego buscar(YearMonth periodo, String campania);

    List<Pliego> listar();

    Evaluacion evaluar(YearMonth periodo, String campania, Map<Metrica, BigDecimal> mediciones);

    Evaluacion evaluar(Pliego pliego, Map<Metrica, BigDecimal> mediciones);
}
