package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.dto.medicion.AltaResponse;
import com.lautaro.multimetrico.dto.medicion.HistoriaResponse;
import com.lautaro.multimetrico.dto.medicion.MedicionesRequest;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

public interface MedicionMapper {

    List<Medicion> aEntidades(MedicionesRequest request);

    AltaResponse aAlta(YearMonth periodo, List<Medicion> registradas);

    HistoriaResponse aHistoria(YearMonth periodo, String legajo, Metrica metrica,
            List<Medicion> historia, Set<Long> vigentes);
}
