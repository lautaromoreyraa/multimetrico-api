package com.lautaro.multimetrico.importacion;

import com.lautaro.multimetrico.importacion.domain.PerfilDeImportacion;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class PerfilEnMemoria implements PerfilRepository {

    private final Map<String, PerfilDeImportacion> perfiles = new ConcurrentHashMap<>();

    private String clave(String nombre) {
        return nombre.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    public PerfilDeImportacion guardar(PerfilDeImportacion perfil) {
        perfiles.put(clave(perfil.nombre()), perfil);
        return perfil;
    }

    @Override
    public Optional<PerfilDeImportacion> porNombre(String nombre) {
        return Optional.ofNullable(perfiles.get(clave(nombre)));
    }

    @Override
    public List<PerfilDeImportacion> todos() {
        return perfiles.values().stream()
                .sorted(Comparator.comparing(PerfilDeImportacion::nombre))
                .toList();
    }
}
