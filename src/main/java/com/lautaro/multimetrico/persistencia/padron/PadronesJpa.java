package com.lautaro.multimetrico.persistencia.padron;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PadronesJpa extends JpaRepository<PadronEntidad, Long> {

    Optional<PadronEntidad> findByPeriodo(YearMonth periodo);

    /**
     * El período va como texto {@code aaaa-mm}, así que el orden alfabético
     * descendente ya es el cronológico descendente. Ver {@code PeriodoComoTexto}.
     */
    List<PadronEntidad> findAllByOrderByPeriodoDesc();
}
