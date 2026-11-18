package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.AliasDeNombre;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AliasRepository extends JpaRepository<AliasDeNombre, Long> {

    Optional<AliasDeNombre> findByNombre(String nombre);

    List<AliasDeNombre> findByLegajoOrderByNombreAsc(String legajo);
}
