package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.evaluacion.Evaluacion;
import com.lautaro.multimetrico.dto.evaluacion.EvaluacionResponse;
import com.lautaro.multimetrico.dto.pliego.PliegoRequest;
import com.lautaro.multimetrico.dto.pliego.PliegoResponse;
import java.util.List;

/**
 * Traduce el pliego entre el JSON y las entidades.
 *
 * <p>{@link #aEntidad} tiene una responsabilidad menos obvia: recolectar los
 * errores de forma que impiden construir el objeto. Ver la implementación.
 */
public interface PliegoMapper {

    /**
     * Arma el pliego desde el JSON, juntando los problemas de forma en vez de
     * explotar en el primero.
     *
     * @throws com.lautaro.multimetrico.exception.PliegoInvalidoException si alguna
     *     banda no se puede construir
     */
    Pliego aEntidad(PliegoRequest request);

    PliegoResponse aResponse(Pliego pliego);

    List<PliegoResponse> aResponses(List<Pliego> pliegos);

    EvaluacionResponse aResponse(Evaluacion evaluacion);
}
