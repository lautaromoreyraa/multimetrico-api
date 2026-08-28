package com.lautaro.multimetrico.dto.error;

import com.lautaro.multimetrico.dto.pliego.Violacion;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Un solo formato de error para toda la API.
 *
 * <p>{@code violaciones} viaja solo en el 422 del pliego y trae lo mismo que
 * {@code detalles} pero con las partes separadas. Existe porque aplastar una
 * violación a texto perdía información que el cliente necesita: la pantalla de
 * carga tiene que marcar en rojo la escala culpable, y con un
 * {@code "TMO: las bandas se pisan…"} la única forma de saber cuál es volver a
 * partir el string por el prefijo — reconstruir a mano algo que el servidor ya
 * tenía estructurado y tiró.
 *
 * <p>El texto sigue viajando en {@code detalles} para quien solo quiera
 * mostrarlo, así que tener las dos formas no rompe a nadie.
 */
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
