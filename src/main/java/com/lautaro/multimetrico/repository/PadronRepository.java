package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.Padron;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Los padrones en la base, uno por período.
 *
 * <p>A diferencia del pliego, el padrón <b>sí se reemplaza</b>: es una lista de
 * gente que cambia dentro del mes —altas, bajas, cambios de equipo—, no un
 * compromiso que haya que poder reproducir. Guardar el padrón de un período borra
 * el anterior y escribe el nuevo entero, y de eso se encarga
 * {@code PadronServiceImpl}.
 */
@Repository
public interface PadronRepository extends JpaRepository<Padron, Long> {

    /**
     * Trae los asesores en la misma consulta.
     *
     * <p>Con {@code open-in-view: false} la transacción ya está cerrada cuando el
     * mapper arma la respuesta, así que una lista perezosa no se puede tocar. Y no
     * hay ningún caso en que se pida un padrón y no se quiera su gente: el padrón
     * <em>es</em> la lista.
     */
    @EntityGraph(attributePaths = "asesores")
    Optional<Padron> findByPeriodo(YearMonth periodo);

    /**
     * Solo los períodos que existen, sin la nómina de cada uno.
     *
     * <p>Acá <b>no</b> va el grafo a propósito: alimenta el selector de período de
     * la pantalla, que muestra una lista de meses. Traer todos los asesores de
     * todos los padrones para leerles la fecha sería cargar la operación entera,
     * varias veces, para no mostrar ni un nombre.
     */
    List<Padron> findAllByOrderByPeriodoDesc();
}
