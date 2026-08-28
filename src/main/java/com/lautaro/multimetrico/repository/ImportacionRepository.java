package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.Importacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * El historial de importaciones.
 *
 * <p>La clave es el UUID que devuelve la API apenas recibe el archivo, no un
 * autoincremental: ver {@link Importacion}.
 */
@Repository
public interface ImportacionRepository extends JpaRepository<Importacion, String> {

    /** La más reciente primero: el historial se mira para ver qué se cargó recién. */
    List<Importacion> findAllByOrderByIniciadaDesc();
}
