package com.lautaro.multimetrico.pliego;

import com.lautaro.multimetrico.pliego.domain.Pliego;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * Implementación en memoria, para tener el motor de bandas andando de punta a
 * punta antes de decidir el esquema.
 *
 * <p>Se reemplaza por JPA + Flyway cuando entren padrón y mediciones, que son
 * las que definen las tablas de verdad. Modelar la base con una sola entidad
 * cargada sería adivinar.
 */
@Repository
public class PliegoEnMemoria implements PliegoRepository {

    private final Map<String, Pliego> pliegos = new ConcurrentHashMap<>();

    private String clave(YearMonth periodo, String campania) {
        return periodo + "|" + campania.trim().toUpperCase();
    }

    @Override
    public Pliego guardar(Pliego pliego) {
        pliegos.put(clave(pliego.periodo(), pliego.campania()), pliego);
        return pliego;
    }

    @Override
    public Optional<Pliego> buscar(YearMonth periodo, String campania) {
        return Optional.ofNullable(pliegos.get(clave(periodo, campania)));
    }

    @Override
    public List<Pliego> todos() {
        return pliegos.values().stream()
                .sorted(Comparator.comparing(Pliego::periodo).reversed()
                        .thenComparing(Pliego::campania))
                .toList();
    }

    @Override
    public boolean existe(YearMonth periodo, String campania) {
        return pliegos.containsKey(clave(periodo, campania));
    }
}
