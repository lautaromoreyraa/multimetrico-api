package com.lautaro.multimetrico.importacion;

import com.lautaro.multimetrico.importacion.domain.Importacion;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class ImportacionEnMemoria implements ImportacionRepository {

    private final Map<String, Importacion> importaciones = new ConcurrentHashMap<>();

    @Override
    public Importacion guardar(Importacion importacion) {
        importaciones.put(importacion.id(), importacion);
        return importacion;
    }

    @Override
    public Optional<Importacion> porId(String id) {
        return Optional.ofNullable(importaciones.get(id));
    }

    @Override
    public List<Importacion> todas() {
        return importaciones.values().stream()
                .sorted(Comparator.comparing(Importacion::iniciada).reversed())
                .toList();
    }
}
