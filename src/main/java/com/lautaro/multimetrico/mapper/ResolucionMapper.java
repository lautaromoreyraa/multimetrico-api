package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import com.lautaro.multimetrico.dto.resolucion.ResolucionResponse;

public interface ResolucionMapper {

    ResolucionResponse aResponse(Resolucion resolucion);
}
