package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.dto.multimetrico.FilaDeMultimetrico;
import com.lautaro.multimetrico.dto.multimetrico.FilaResponse;
import com.lautaro.multimetrico.dto.multimetrico.Multimetrico;
import com.lautaro.multimetrico.dto.multimetrico.MultimetricoResponse;

public interface MultimetricoMapper {

    MultimetricoResponse aResponse(Multimetrico multimetrico);

    FilaResponse aResponse(FilaDeMultimetrico fila);
}
