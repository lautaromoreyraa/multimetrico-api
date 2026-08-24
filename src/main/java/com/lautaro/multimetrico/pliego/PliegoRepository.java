package com.lautaro.multimetrico.pliego;

import com.lautaro.multimetrico.pliego.domain.Pliego;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * El puerto de persistencia del pliego.
 *
 * <p>Es una interfaz y no un {@code JpaRepository} directo para que el dominio
 * y el validador —que es donde está el valor de este proyecto— se puedan probar
 * sin levantar una base. La implementación de hoy guarda en memoria; la de JPA
 * entra detrás de esta misma firma sin tocar nada más.
 */
public interface PliegoRepository {

    Pliego guardar(Pliego pliego);

    Optional<Pliego> buscar(YearMonth periodo, String campania);

    List<Pliego> todos();

    boolean existe(YearMonth periodo, String campania);
}
