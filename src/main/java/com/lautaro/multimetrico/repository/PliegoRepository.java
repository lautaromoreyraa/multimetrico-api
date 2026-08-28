package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.Pliego;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Los pliegos en la base.
 *
 * <p>Todo se busca por {@code campaniaClave} y no por {@code campania}: MySQL
 * compara sin distinguir mayúsculas y H2 sí las distingue, así que buscar por el
 * texto original haría que "CONVERGENTES" y "Convergentes" sean la misma campaña
 * en producción y dos distintas en los tests. La columna normalizada saca a la
 * collation de la ecuación.
 *
 * <p>Acá no hay ninguna regla de negocio: que un pliego cargado <b>no se pise</b>
 * lo decide {@code PliegoServiceImpl}, consultando {@link #existsByPeriodoAndCampaniaClave}
 * antes de guardar. Si el repositorio además rechazara por su cuenta habría dos
 * lugares que pueden decir que no, y uno de los dos se va a olvidar.
 *
 * <h2>Por qué las lecturas traen las escalas y las bandas</h2>
 *
 * <p>La aplicación corre con {@code open-in-view: false}: cuando el mapper arma
 * la respuesta, la transacción ya se cerró. Sin el {@link EntityGraph}, un pliego
 * leído acá saldría con {@code escalas} sin cargar y tocar la lista tiraría
 * {@code LazyInitializationException} — no en un caso raro, sino en el camino
 * principal de todos los endpoints del pliego.
 *
 * <p>Traerlas en la misma consulta también evita el N+1: un pliego con cinco
 * escalas de ocho bandas serían cuarenta y seis consultas en vez de una. El
 * anidado {@code escalas.bandas} es una lista adentro de otra y no dos listas del
 * mismo dueño, que es lo único que Hibernate no puede resolver de una vez.
 */
@Repository
public interface PliegoRepository extends JpaRepository<Pliego, Long> {

    @EntityGraph(attributePaths = {"escalas", "escalas.bandas"})
    Optional<Pliego> findByPeriodoAndCampaniaClave(YearMonth periodo, String campaniaClave);

    /** No necesita el grafo: pregunta por la existencia, no por el contenido. */
    boolean existsByPeriodoAndCampaniaClave(YearMonth periodo, String campaniaClave);

    /** El más nuevo primero, que es el que se está usando. */
    @EntityGraph(attributePaths = {"escalas", "escalas.bandas"})
    List<Pliego> findAllByOrderByPeriodoDescCampaniaAsc();
}
