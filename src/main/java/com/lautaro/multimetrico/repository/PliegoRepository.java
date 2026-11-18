package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.Pliego;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PliegoRepository extends JpaRepository<Pliego, Long> {

    @EntityGraph(attributePaths = {"escalas", "escalas.bandas"})
    Optional<Pliego> findByPeriodoAndCampaniaClave(YearMonth periodo, String campaniaClave);

    boolean existsByPeriodoAndCampaniaClave(YearMonth periodo, String campaniaClave);

    @EntityGraph(attributePaths = {"escalas", "escalas.bandas"})
    List<Pliego> findAllByOrderByPeriodoDescCampaniaAsc();
}
