package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.dto.multimetrico.FilaDeMultimetrico;
import com.lautaro.multimetrico.dto.multimetrico.FilaResponse;
import com.lautaro.multimetrico.dto.multimetrico.Multimetrico;
import com.lautaro.multimetrico.dto.multimetrico.MultimetricoResponse;

/** Le da forma de respuesta HTTP al multimétrico ya calculado. */
public interface MultimetricoMapper {

    MultimetricoResponse aResponse(Multimetrico multimetrico);

    FilaResponse aResponse(FilaDeMultimetrico fila);
}
