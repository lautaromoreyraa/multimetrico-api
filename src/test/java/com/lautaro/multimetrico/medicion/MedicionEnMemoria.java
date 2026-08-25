package com.lautaro.multimetrico.medicion;

import com.lautaro.multimetrico.medicion.domain.Medicion;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Las mediciones en memoria. Hoy es un <b>doble de test</b>, no la
 * implementación del sistema.
 *
 * <p>Vivía en {@code src/main} y era un {@code @Repository} hasta que entró la
 * persistencia. Se mudó al árbol de tests en vez de perfilarse, y el motivo es
 * que no puede haber dos beans de {@link MedicionRepository}: con las dos
 * anotadas Spring no arranca, y con un perfil que elija una de las dos
 * aparecería una configuración capaz de hacer que la aplicación ande en los
 * tests y no en producción. Acá Spring ni la ve, así que esa elección no existe.
 *
 * <p>Se conserva porque {@code MedicionServiceTest} la usa para probar el
 * servicio sin levantar el contexto: siete tests que corren en milisegundos.
 *
 * <p>Guarda una lista por período y <b>solo agrega al final</b>: la posición en
 * la lista <em>es</em> el orden de carga, que es de lo que depende el desempate
 * entre dos mediciones del mismo corte. Por eso {@link CopyOnWriteArrayList} y
 * no un {@code Set} ni un mapa por clave: dos mediciones de la misma métrica y
 * el mismo asesor no son un duplicado a deduplicar, son dos hechos. La
 * implementación JPA consigue lo mismo ordenando por id.
 */
class MedicionEnMemoria implements MedicionRepository {

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
