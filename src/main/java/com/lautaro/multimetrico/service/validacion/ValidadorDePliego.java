package com.lautaro.multimetrico.service.validacion;

import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.pliego.Violacion;
import java.util.List;

public interface ValidadorDePliego {

    List<Violacion> validar(Pliego pliego);
}
