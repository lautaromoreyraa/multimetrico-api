package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.dto.medicion.AltaResponse;
import com.lautaro.multimetrico.dto.medicion.HistoriaResponse;
import com.lautaro.multimetrico.dto.medicion.MedicionesRequest;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/** Traduce las mediciones entre el JSON y las entidades. */
public interface MedicionMapper {

    /**
     * Desarma el lote en mediciones sueltas.
     *
     * <p>El período y la fecha de corte del lote bajan a cada ítem; el ítem que
     * trae su propia fecha la conserva.
     */
    List<Medicion> aEntidades(MedicionesRequest request);

    AltaResponse aAlta(YearMonth periodo, List<Medicion> registradas);

    /**
     * La historia con cada fila marcada como vigente o superada.
     *
     * @param vigentes los ids que están valiendo, calculados por
     *     {@code MedicionService.vigentesEn}
     */
    HistoriaResponse aHistoria(YearMonth periodo, String legajo, Metrica metrica,
            List<Medicion> historia, Set<Long> vigentes);
}
