package com.lautaro.multimetrico.dto.resolucion;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ResolucionRequest(
        @NotEmpty(message = "hace falta al menos un nombre") List<String> nombres) {
}
