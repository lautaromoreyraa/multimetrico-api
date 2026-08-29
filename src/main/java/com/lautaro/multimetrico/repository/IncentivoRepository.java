package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.Incentivo;
import java.time.YearMonth;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Los incentivos de un período.
 *
 * <p><b>El {@code ORDER BY id} es parte del contrato, no una preferencia.</b> Es
 * lo que hace que, entre dos cargas del mismo asesor, gane la última: quien
 * consume esta lista se queda con la de más abajo. Ordenar por cualquier otra
 * cosa —incluida la fecha de creación, que puede empatar dentro del mismo
 * milisegundo— rompe esa regla sin que ningún test evidente se caiga.
 *
 * <p>Es la misma trampa que ya costó un fallo en {@code MedicionRepository}, y
 * está anotada en el handoff.
 */
public interface IncentivoRepository extends JpaRepository<Incentivo, Long> {

    @Query("select i from Incentivo i where i.periodo = :periodo order by i.id asc")
    List<Incentivo> delPeriodo(@Param("periodo") String periodo);

    default List<Incentivo> delPeriodo(YearMonth periodo) {
        return delPeriodo(periodo.toString());
    }
}
