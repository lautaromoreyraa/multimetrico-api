package com.lautaro.multimetrico.service.medicion.impl;

import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.dto.medicion.MedicionVigente;
import com.lautaro.multimetrico.dto.medicion.MedicionesVigentes;
import com.lautaro.multimetrico.enumeration.Metrica;
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

/**
 * Las mediciones: guardar la historia y decidir cuál de todas es la que vale.
 *
 * <p>Acá vive el desempate, que antes era un método de {@code MedicionesVigentes}.
 * El criterio: <b>gana la del corte más reciente y, empatado el corte, la última
 * cargada</b>, porque dos mediciones con la misma fecha son dos cargas del mismo
 * archivo y la segunda es la corrección.
 *
 * <p>Eso obliga a recorrer la historia <b>en orden de carga</b>, tratando cada
 * medición como candidata a desplazar a la que quedó atrás. El repositorio
 * devuelve en ese orden y no es negociable: ver {@code MedicionRepository}.
 */
@Service
public class MedicionServiceImpl implements MedicionService {

    private final MedicionRepository repositorio;

    public MedicionServiceImpl(MedicionRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Guarda el lote entero en una transacción.
     *
     * <p>El lote es un archivo semanal: o entra completo o no entra ninguna fila.
     * Media importación guardada es peor que ninguna, porque el informe diría que
     * entraron N y en la base habría menos, y nadie tendría cómo saber cuáles
     * faltan.
     */
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

    /**
     * Se queda con la última de cada métrica.
     *
     * <p>{@code historia} tiene que venir en orden de carga. Ver
     * {@link Medicion#desplazaA}.
     */
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

    /**
     * El valor vigente de cada métrica, con su corte pegado.
     *
     * <p>Una métrica ausente queda ausente: no se completa con cero. Un cero es un
     * desempeño malísimo y un faltante es un archivo que todavía no llegó.
     */
    private static MedicionesVigentes armar(List<Medicion> historia) {
        Map<Metrica, MedicionVigente> porMetrica = new EnumMap<>(Metrica.class);
        ultimasPorMetrica(historia).forEach((metrica, medicion) ->
                porMetrica.put(metrica, new MedicionVigente(medicion.valor(), medicion.fechaDeCorte())));

        return porMetrica.isEmpty() ? MedicionesVigentes.ninguna() : new MedicionesVigentes(porMetrica);
    }

    /** Agrupa por legajo conservando el orden de carga dentro de cada uno. */
    private static Map<String, List<Medicion>> porLegajo(List<Medicion> historia) {
        Map<String, List<Medicion>> agrupadas = new LinkedHashMap<>();
        for (Medicion medicion : historia) {
            agrupadas.computeIfAbsent(Medicion.clave(medicion.legajo()), clave -> new ArrayList<>())
                    .add(medicion);
        }
        return agrupadas;
    }
}
