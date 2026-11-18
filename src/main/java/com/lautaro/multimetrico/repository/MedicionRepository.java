package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.Medicion;
import java.time.YearMonth;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicionRepository extends JpaRepository<Medicion, Long> {

    List<Medicion> findByPeriodoOrderByIdAsc(YearMonth periodo);

    List<Medicion> findByPeriodoAndLegajoClaveOrderByIdAsc(YearMonth periodo, String legajoClave);
}
