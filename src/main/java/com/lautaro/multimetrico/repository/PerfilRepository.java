package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Los perfiles guardados, uno por fuente.
 *
 * <p>El perfil es <b>dato, no código</b>, y esta tabla es lo que hace cierta esa
 * frase. Cuando la empresa cambie el reporte, se edita un registro; no se
 * recompila ni se redespliega nada.
 */
@Repository
public interface PerfilRepository extends JpaRepository<PerfilDeImportacion, Long> {

    Optional<PerfilDeImportacion> findByNombreClave(String nombreClave);

    List<PerfilDeImportacion> findAllByOrderByNombreAsc();
}
