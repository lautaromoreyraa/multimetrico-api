package com.lautaro.multimetrico.pliego.web;

import com.lautaro.multimetrico.pliego.PliegoYaCargadoException;
import com.lautaro.multimetrico.pliego.validacion.PliegoInvalidoException;
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

    public record ErrorResponse(Instant momento, int estado, String mensaje, List<String> detalles) {

        static ErrorResponse de(HttpStatus estado, String mensaje, List<String> detalles) {
            return new ErrorResponse(Instant.now(), estado.value(), mensaje, detalles);
        }
    }

    @ExceptionHandler(PliegoInvalidoException.class)
    public ResponseEntity<ErrorResponse> pliegoInvalido(PliegoInvalidoException e) {
        List<String> detalles = e.violaciones().stream().map(Object::toString).toList();
        return ResponseEntity.unprocessableEntity()
                .body(ErrorResponse.de(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), detalles));
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
