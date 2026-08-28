package com.lautaro.multimetrico.mapper.impl;

import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.dto.medicion.AltaResponse;
import com.lautaro.multimetrico.dto.medicion.HistoriaResponse;
import com.lautaro.multimetrico.dto.medicion.MedicionesRequest;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.mapper.MedicionMapper;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** El mapper de mediciones, escrito a mano. */
@Component
public class MedicionMapperImpl implements MedicionMapper {

    @Override
    public List<Medicion> aEntidades(MedicionesRequest request) {
        return request.mediciones().stream()
                .map(item -> new Medicion(
                        item.legajo(),
                        item.metrica(),
                        request.periodo(),
                        item.valor(),
                        item.fechaDeCorte() == null ? request.fechaDeCorte() : item.fechaDeCorte(),
                        request.origenImportacion()))
                .toList();
    }

    @Override
    public AltaResponse aAlta(YearMonth periodo, List<Medicion> registradas) {
        Map<Metrica, Long> porMetrica = registradas.stream()
                .collect(Collectors.groupingBy(Medicion::metrica,
                        () -> new EnumMap<>(Metrica.class), Collectors.counting()));

        return new AltaResponse(
                periodo,
                registradas.size(),
                (int) registradas.stream().map(Medicion::legajoClave).distinct().count(),
                porMetrica,
                registradas.stream().map(Medicion::fechaDeCorte).distinct().sorted().toList());
    }

    @Override
    public HistoriaResponse aHistoria(YearMonth periodo, String legajo, Metrica metrica,
            List<Medicion> historia, Set<Long> vigentes) {

        List<HistoriaResponse.MedicionResponse> filas = historia.stream()
                .map(medicion -> new HistoriaResponse.MedicionResponse(
                        medicion.legajo(),
                        medicion.metrica(),
                        medicion.valor(),
                        medicion.fechaDeCorte(),
                        medicion.origenImportacion(),
                        // Por id y no por valor: dos cargas iguales del mismo corte
                        // tienen todos los campos idénticos y solo el id las separa.
                        vigentes.contains(medicion.id())))
                .toList();

        return new HistoriaResponse(periodo, legajo, metrica, filas.size(), filas);
    }
}
