package com.lautaro.multimetrico.persistencia.padron;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface AliasJpa extends JpaRepository<AliasEntidad, Long> {

    Optional<AliasEntidad> findByNombre(String nombre);

    List<AliasEntidad> findByLegajoOrderByNombreAsc(String legajo);
}
