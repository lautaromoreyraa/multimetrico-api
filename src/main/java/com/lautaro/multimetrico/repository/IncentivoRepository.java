package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.Incentivo;
import java.time.YearMonth;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncentivoRepository extends JpaRepository<Incentivo, Long> {

    @Query("select i from Incentivo i where i.periodo = :periodo order by i.id asc")
    List<Incentivo> delPeriodo(@Param("periodo") String periodo);

    default List<Incentivo> delPeriodo(YearMonth periodo) {
        return delPeriodo(periodo.toString());
    }
}
