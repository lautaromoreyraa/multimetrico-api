package com.lautaro.multimetrico.persistencia.medicion;

import java.time.YearMonth;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <h2>El ORDER BY de este archivo es parte del contrato del dominio</h2>
 *
 * <p>Las dos consultas ordenan <b>por id</b>, que es el orden de carga.
 * {@code ORDER BY fechaDeCorte} sería la traducción intuitiva de "la historia del
 * mes" y estaría mal.
 *
 * <p>El motivo: dos mediciones con la misma fecha de corte son el mismo archivo
 * subido dos veces, y la segunda es la corrección de la primera. Por eso gana la
 * última cargada —{@code Medicion.desplazaA} desempata a favor de la que llega
 * después— y por eso {@code MedicionesVigentes.de} exige recibir la historia en
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
 */
interface MedicionesJpa extends JpaRepository<MedicionEntidad, Long> {

    List<MedicionEntidad> findByPeriodoOrderByIdAsc(YearMonth periodo);

    List<MedicionEntidad> findByPeriodoAndLegajoClaveOrderByIdAsc(YearMonth periodo, String legajoClave);
}
