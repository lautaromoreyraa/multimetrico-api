package com.lautaro.multimetrico.dto.error;

import com.lautaro.multimetrico.dto.pliego.Violacion;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

public record ErrorResponse(
        Instant momento,
        int estado,
        String mensaje,
        List<String> detalles,
        List<ViolacionResponse> violaciones) {

    public static ErrorResponse de(HttpStatus estado, String mensaje, List<String> detalles) {
        return new ErrorResponse(Instant.now(), estado.value(), mensaje, detalles, null);
    }

    public static ErrorResponse conViolaciones(HttpStatus estado, String mensaje, List<Violacion> violaciones) {
        return new ErrorResponse(
                Instant.now(),
                estado.value(),
                mensaje,
                violaciones.stream().map(Object::toString).toList(),
                violaciones.stream().map(ViolacionResponse::de).toList());
    }
}
