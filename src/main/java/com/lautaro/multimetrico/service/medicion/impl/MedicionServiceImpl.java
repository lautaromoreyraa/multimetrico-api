package com.lautaro.multimetrico.service.medicion.impl;

import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.dto.medicion.AltaResponse;
import com.lautaro.multimetrico.dto.medicion.HistoriaResponse;
import com.lautaro.multimetrico.dto.medicion.MedicionVigente;
import com.lautaro.multimetrico.dto.medicion.MedicionesRequest;
import com.lautaro.multimetrico.dto.medicion.MedicionesVigentes;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.mapper.MedicionMapper;
import com.lautaro.multimetrico.repository.MedicionRepository;
import com.lautaro.multimetrico.service.medicion.MedicionService;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicionServiceImpl implements MedicionService {

    private final MedicionRepository repositorio;
    private final MedicionMapper mapper;

    public MedicionServiceImpl(MedicionRepository repositorio, MedicionMapper mapper) {
        this.repositorio = repositorio;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public List<Medicion> registrar(List<Medicion> mediciones) {
        if (mediciones == null || mediciones.isEmpty()) {
            throw new IllegalArgumentException("El lote no trae ninguna medición");
        }
        return repositorio.saveAll(mediciones);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicion> historia(YearMonth periodo, String legajo, Metrica metrica) {
        List<Medicion> historia = legajo == null || legajo.isBlank()
                ? repositorio.findByPeriodoOrderByIdAsc(periodo)
                : repositorio.findByPeriodoAndLegajoClaveOrderByIdAsc(periodo, Medicion.clave(legajo));

        return metrica == null
                ? historia
                : historia.stream().filter(medicion -> medicion.metrica() == metrica).toList();
    }

    @Override
    public Set<Long> vigentesEn(List<Medicion> historia) {
        Set<Long> ids = new HashSet<>();
        porLegajo(historia).values().forEach(deUno ->
                ultimasPorMetrica(deUno).values().forEach(medicion -> ids.add(medicion.id())));
        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public MedicionesVigentes vigentesDe(YearMonth periodo, String legajo) {
        return armar(repositorio.findByPeriodoAndLegajoClaveOrderByIdAsc(periodo, Medicion.clave(legajo)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, MedicionesVigentes> vigentesDelPeriodo(YearMonth periodo) {
        Map<String, MedicionesVigentes> vigentes = new LinkedHashMap<>();
        porLegajo(repositorio.findByPeriodoOrderByIdAsc(periodo))
                .forEach((legajo, historia) -> vigentes.put(legajo, armar(historia)));
        return vigentes;
    }

    private static Map<Metrica, Medicion> ultimasPorMetrica(List<Medicion> historia) {
        Map<Metrica, Medicion> vigentes = new EnumMap<>(Metrica.class);
        for (Medicion medicion : historia) {
            Medicion actual = vigentes.get(medicion.metrica());
            if (actual == null || medicion.desplazaA(actual)) {
                vigentes.put(medicion.metrica(), medicion);
            }
        }
        return vigentes;
    }

    private static MedicionesVigentes armar(List<Medicion> historia) {
        Map<Metrica, MedicionVigente> porMetrica = new EnumMap<>(Metrica.class);
        ultimasPorMetrica(historia).forEach((metrica, medicion) ->
                porMetrica.put(metrica, new MedicionVigente(medicion.valor(), medicion.fechaDeCorte())));

        return porMetrica.isEmpty() ? MedicionesVigentes.ninguna() : new MedicionesVigentes(porMetrica);
    }

    private static Map<String, List<Medicion>> porLegajo(List<Medicion> historia) {
        Map<String, List<Medicion>> agrupadas = new LinkedHashMap<>();
        for (Medicion medicion : historia) {
            agrupadas.computeIfAbsent(Medicion.clave(medicion.legajo()), clave -> new ArrayList<>())
                    .add(medicion);
        }
        return agrupadas;
    }

    @Override
    @Transactional
    public AltaResponse registrar(MedicionesRequest request) {
        return mapper.aAlta(request.periodo(), registrar(mapper.aEntidades(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public HistoriaResponse historiaDelPeriodo(YearMonth periodo, String legajo, Metrica metrica) {
        List<Medicion> historia = historia(periodo, legajo, metrica);
        return mapper.aHistoria(periodo, legajo, metrica, historia, vigentesEn(historia));
    }
}
