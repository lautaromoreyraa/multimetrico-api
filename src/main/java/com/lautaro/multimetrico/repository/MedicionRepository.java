package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.Medicion;
import java.time.YearMonth;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * La historia de mediciones.
 *
 * <h2>El ORDER BY de este archivo es parte del contrato del dominio</h2>
 *
 * <p>Las dos consultas ordenan <b>por id</b>, que es el orden de carga.
 * {@code ORDER BY fechaDeCorte} sería la traducción intuitiva de "la historia del
 * mes" y estaría mal.
 *
 * <p>El motivo: dos mediciones con la misma fecha de corte son el mismo archivo
 * subido dos veces, y la segunda es la corrección de la primera. Por eso gana la
 * última cargada —{@link Medicion#desplazaA} desempata a favor de la que llega
 * después— y por eso el cálculo del valor vigente exige recibir la historia en
 * orden de carga.
 *
 * <p>Lo peligroso es cómo falla si alguien lo cambia: ordenando por fecha de
 * corte, el motor devuelve las dos filas del mismo día en cualquier orden y la
 * corrección puede quedar primera, con lo cual la que sobrevive es la vieja. No
 * salta ninguna excepción, ninguna consulta devuelve cero filas y ningún log
 * dice nada: el multimétrico simplemente muestra el número anterior. Ese es
 * exactamente el defecto de la planilla que este módulo vino a corregir, de
 * vuelta.
 *
 * <p>Los índices de la migración terminan en {@code id} justamente para que este
 * orden salga del índice y no de una ordenación aparte.
 *
 * <p>De {@link JpaRepository} solo se usan las altas y las lecturas. Una medición
 * es un hecho fechado: corregir un número es cargar otro, no pisar el anterior.
 */
@Repository
public interface MedicionRepository extends JpaRepository<Medicion, Long> {

    /** Toda la historia del mes, de todos los asesores, en orden de carga. */
    List<Medicion> findByPeriodoOrderByIdAsc(YearMonth periodo);

    /** La historia del mes de un asesor, en orden de carga. */
    List<Medicion> findByPeriodoAndLegajoClaveOrderByIdAsc(YearMonth periodo, String legajoClave);
}
