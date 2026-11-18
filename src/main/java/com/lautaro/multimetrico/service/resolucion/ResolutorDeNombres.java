package com.lautaro.multimetrico.service.resolucion;

import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import java.util.List;

public interface ResolutorDeNombres {

    Resolucion resolver(Padron padron, List<String> nombres);
}
