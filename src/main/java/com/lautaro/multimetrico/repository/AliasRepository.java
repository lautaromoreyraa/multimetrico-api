package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.AliasDeNombre;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Los nombres raros que alguien ya confirmó a qué legajo corresponden.
 *
 * <p>Se busca por el nombre <b>ya normalizado</b> —mayúsculas, sin acentos, solo
 * A-Z 0-9 y espacio—, que es como se guarda. Ver {@code NombreNormalizado}.
 */
@Repository
public interface AliasRepository extends JpaRepository<AliasDeNombre, Long> {

    Optional<AliasDeNombre> findByNombre(String nombre);

    List<AliasDeNombre> findByLegajoOrderByNombreAsc(String legajo);
}
