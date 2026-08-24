package com.lautaro.multimetrico.padron;

import com.lautaro.multimetrico.padron.domain.NombreNormalizado;
import java.util.List;
import java.util.Optional;

/**
 * Los nombres raros que alguien ya confirmó a qué legajo corresponden.
 *
 * <p>Vive fuera del padrón y no tiene período: es conocimiento que se acumula.
 * Si en marzo alguien confirmó que cierto texto mal codificado es tal legajo, en
 * abril el sistema no vuelve a preguntar. Esa es la diferencia entre una
 * herramienta que aprende y una que hace corregir lo mismo todos los meses.
 */
public interface RegistroDeAlias {

    Optional<String> legajoDe(NombreNormalizado nombre);

    void confirmar(NombreNormalizado nombre, String legajo);

    List<String> aliasDe(String legajo);
}
