package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.evaluacion.Evaluacion;
import com.lautaro.multimetrico.dto.evaluacion.EvaluacionResponse;
import com.lautaro.multimetrico.dto.pliego.PliegoRequest;
import com.lautaro.multimetrico.dto.pliego.PliegoResponse;
import java.util.List;

public interface PliegoMapper {

    Pliego aEntidad(PliegoRequest request);

    PliegoResponse aResponse(Pliego pliego);

    List<PliegoResponse> aResponses(List<Pliego> pliegos);

    EvaluacionResponse aResponse(Evaluacion evaluacion);
}
