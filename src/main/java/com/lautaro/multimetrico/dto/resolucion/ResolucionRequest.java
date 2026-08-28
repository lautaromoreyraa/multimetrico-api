package com.lautaro.multimetrico.dto.resolucion;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Los nombres tal como vienen en la columna del archivo, sin tocar. */
public record ResolucionRequest(
        @NotEmpty(message = "hace falta al menos un nombre") List<String> nombres) {
}
