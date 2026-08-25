package com.lautaro.multimetrico.pliego.web;

import com.lautaro.multimetrico.pliego.PliegoYaCargadoException;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import com.lautaro.multimetrico.pliego.validacion.PliegoInvalidoException;
import com.lautaro.multimetrico.pliego.validacion.Violacion;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Un solo formato de error para toda la API.
 *
 * <p>El caso que importa es el pliego inválido: responde 422 con <b>todas</b>
 * las violaciones. Es deliberado que no corte en la primera — quien carga el
 * pliego del mes tiene que poder arreglarlo de una pasada, no descubrir el
 * siguiente problema recién después de corregir el anterior.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    public record ErrorResponse(
            Instant momento,
            int estado,
            String mensaje,
            List<String> detalles,
            List<ViolacionResponse> violaciones) {

        static ErrorResponse de(HttpStatus estado, String mensaje, List<String> detalles) {
            return new ErrorResponse(Instant.now(), estado.value(), mensaje, detalles, null);
        }
    }

    /**
     * Una violación con sus partes separadas.
     *
     * <p>Existe porque aplastarla a texto perdía información que el cliente
     * necesita. La pantalla de carga tiene que marcar en rojo la escala culpable,
     * y con un `"TMO: las bandas se pisan…"` la única forma de saber cuál es
     * volver a partir el string por el prefijo — reconstruir a mano algo que el
     * servidor ya tenía estructurado y tiró.
     *
     * <p>El texto sigue viajando en {@code detalles} para quien solo quiera
     * mostrarlo, así que agregar esto no rompe a nadie.
     */
    public record ViolacionResponse(Metrica metrica, Violacion.Tipo tipo, String mensaje) {

        static ViolacionResponse de(Violacion violacion) {
            return new ViolacionResponse(violacion.metrica(), violacion.tipo(), violacion.mensaje());
        }
    }

    @ExceptionHandler(PliegoInvalidoException.class)
    public ResponseEntity<ErrorResponse> pliegoInvalido(PliegoInvalidoException e) {
        List<String> detalles = e.violaciones().stream().map(Object::toString).toList();
        List<ViolacionResponse> violaciones = e.violaciones().stream().map(ViolacionResponse::de).toList();

        return ResponseEntity.unprocessableEntity().body(new ErrorResponse(
                Instant.now(), HttpStatus.UNPROCESSABLE_ENTITY.value(), e.getMessage(), detalles, violaciones));
    }

    @ExceptionHandler(PliegoYaCargadoException.class)
    public ResponseEntity<ErrorResponse> yaCargado(PliegoYaCargadoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.de(HttpStatus.CONFLICT, e.getMessage(), List.of()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.de(HttpStatus.NOT_FOUND, e.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> cuerpoInvalido(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.de(HttpStatus.BAD_REQUEST, "La petición tiene campos inválidos", detalles));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> argumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.de(HttpStatus.BAD_REQUEST, e.getMessage(), List.of()));
    }
}
