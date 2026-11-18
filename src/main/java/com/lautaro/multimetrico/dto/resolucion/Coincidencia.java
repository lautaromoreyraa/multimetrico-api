package com.lautaro.multimetrico.dto.resolucion;

import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.enumeration.Estrategia;

public record Coincidencia(String textoOriginal, AsesorResponse asesor, Estrategia estrategia) {
}
