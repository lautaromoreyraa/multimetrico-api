package com.lautaro.multimetrico.padron;

import com.lautaro.multimetrico.padron.domain.NombreNormalizado;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class AliasEnMemoria implements RegistroDeAlias {

    private final Map<String, String> porNombre = new ConcurrentHashMap<>();

    @Override
    public Optional<String> legajoDe(NombreNormalizado nombre) {
        return Optional.ofNullable(porNombre.get(nombre.valor()));
    }

    @Override
    public void confirmar(NombreNormalizado nombre, String legajo) {
        porNombre.put(nombre.valor(), legajo);
    }

    @Override
    public List<String> aliasDe(String legajo) {
        return porNombre.entrySet().stream()
                .filter(entrada -> entrada.getValue().equals(legajo))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }
}
