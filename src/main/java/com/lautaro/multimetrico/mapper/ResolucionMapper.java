package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import com.lautaro.multimetrico.dto.resolucion.ResolucionResponse;

/** Arma el informe de resolución que se muestra antes de importar. */
public interface ResolucionMapper {

    ResolucionResponse aResponse(Resolucion resolucion);
}
