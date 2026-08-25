package com.lautaro.multimetrico.persistencia.importacion;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PerfilesJpa extends JpaRepository<PerfilEntidad, Long> {

    Optional<PerfilEntidad> findByNombreClave(String nombreClave);

    List<PerfilEntidad> findAllByOrderByNombreAsc();
}
