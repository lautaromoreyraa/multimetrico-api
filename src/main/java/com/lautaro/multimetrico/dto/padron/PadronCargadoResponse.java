package com.lautaro.multimetrico.dto.padron;

import java.util.List;

public record PadronCargadoResponse(String periodo, int asesores, List<String> lideres) {
}
