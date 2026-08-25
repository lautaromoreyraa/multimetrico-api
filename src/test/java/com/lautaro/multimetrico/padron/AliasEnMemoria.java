package com.lautaro.multimetrico.padron;

import com.lautaro.multimetrico.padron.domain.NombreNormalizado;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El registro de alias en memoria, hoy <b>doble de test</b>.
 *
 * <p>Mismo motivo que {@code MedicionEnMemoria}: no puede haber dos beans de
 * {@link RegistroDeAlias}, así que la versión en memoria se fue al árbol de
 * tests en vez de quedar detrás de un perfil. La usa
 * {@code ResolutorDeNombresTest}, que prueba las cuatro estrategias de
 * resolución sin levantar Spring.
 */
class AliasEnMemoria implements RegistroDeAlias {

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
