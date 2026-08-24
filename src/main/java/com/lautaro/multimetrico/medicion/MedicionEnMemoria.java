package com.lautaro.multimetrico.medicion;

import com.lautaro.multimetrico.medicion.domain.Medicion;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

/**
 * Implementación en memoria, igual que la del pliego y la del padrón, hasta que
 * entre la base.
 *
 * <p>Guarda una lista por período y <b>solo agrega al final</b>: la posición en
 * la lista <em>es</em> el orden de carga, que es de lo que depende el desempate
 * entre dos mediciones del mismo corte. Por eso {@link CopyOnWriteArrayList} y
 * no un {@code Set} ni un mapa por clave: dos mediciones de la misma métrica y
 * el mismo asesor no son un duplicado a deduplicar, son dos hechos.
 */
@Repository
public class MedicionEnMemoria implements MedicionRepository {

    private final Map<YearMonth, List<Medicion>> porPeriodo = new ConcurrentHashMap<>();

    @Override
    public List<Medicion> guardar(List<Medicion> mediciones) {
        for (Medicion medicion : mediciones) {
            porPeriodo.computeIfAbsent(medicion.periodo(), periodo -> new CopyOnWriteArrayList<>())
                    .add(medicion);
        }
        return List.copyOf(mediciones);
    }

    @Override
    public List<Medicion> historia(YearMonth periodo) {
        return List.copyOf(porPeriodo.getOrDefault(periodo, List.of()));
    }

    @Override
    public List<Medicion> historiaDe(YearMonth periodo, String legajo) {
        return historia(periodo).stream()
                .filter(medicion -> medicion.esDe(legajo))
                .toList();
    }
}
