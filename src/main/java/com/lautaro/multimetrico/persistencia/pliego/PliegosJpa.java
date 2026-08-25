package com.lautaro.multimetrico.persistencia.pliego;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * La consulta cruda. No es el puerto: el puerto es
 * {@code PliegoRepository}, y {@link PliegoEnBase} es lo que lo implementa.
 *
 * <p>Están separados para que el dominio no vea nunca un {@code JpaRepository}
 * ni una entidad. Este de acá habla de {@link PliegoEntidad}; el otro habla de
 * {@code Pliego}.
 */
interface PliegosJpa extends JpaRepository<PliegoEntidad, Long> {

    Optional<PliegoEntidad> findByPeriodoAndCampaniaClave(YearMonth periodo, String campaniaClave);

    boolean existsByPeriodoAndCampaniaClave(YearMonth periodo, String campaniaClave);

    /** El más nuevo primero, que es el que se está usando. */
    List<PliegoEntidad> findAllByOrderByPeriodoDescCampaniaAsc();
}
