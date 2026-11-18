package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.Padron;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PadronRepository extends JpaRepository<Padron, Long> {

    @EntityGraph(attributePaths = "asesores")
    Optional<Padron> findByPeriodo(YearMonth periodo);

    List<Padron> findAllByOrderByPeriodoDesc();
}
