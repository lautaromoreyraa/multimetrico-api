package com.lautaro.multimetrico.service.medicion;

import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.dto.medicion.AltaResponse;
import com.lautaro.multimetrico.dto.medicion.HistoriaResponse;
import com.lautaro.multimetrico.dto.medicion.MedicionesRequest;
import com.lautaro.multimetrico.dto.medicion.MedicionesVigentes;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MedicionService {

    List<Medicion> registrar(List<Medicion> mediciones);

    List<Medicion> historia(YearMonth periodo, String legajo, Metrica metrica);

    Set<Long> vigentesEn(List<Medicion> historia);

    MedicionesVigentes vigentesDe(YearMonth periodo, String legajo);

    Map<String, MedicionesVigentes> vigentesDelPeriodo(YearMonth periodo);

    AltaResponse registrar(MedicionesRequest request);

    HistoriaResponse historiaDelPeriodo(YearMonth periodo, String legajo, Metrica metrica);
}
