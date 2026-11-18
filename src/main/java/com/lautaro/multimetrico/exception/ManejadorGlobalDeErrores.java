package com.lautaro.multimetrico.exception;

import com.lautaro.multimetrico.dto.error.ErrorResponse;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorGlobalDeErrores {

    @ExceptionHandler(PliegoInvalidoException.class)
    public ResponseEntity<ErrorResponse> pliegoInvalido(PliegoInvalidoException e) {
        return ResponseEntity.unprocessableEntity().body(ErrorResponse.conViolaciones(
                HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e.violaciones()));
    }

    @ExceptionHandler(PlanillaRechazadaException.class)
    public ResponseEntity<ErrorResponse> planillaRechazada(PlanillaRechazadaException e) {
        List<String> detalles = e.desajustes().stream().map(Object::toString).toList();
        return ResponseEntity.unprocessableEntity()
                .body(ErrorResponse.de(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), detalles));
    }

    @ExceptionHandler(PliegoYaCargadoException.class)
    public ResponseEntity<ErrorResponse> yaCargado(PliegoYaCargadoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.de(HttpStatus.CONFLICT, e.getMessage(), List.of()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.de(HttpStatus.NOT_FOUND, e.getMessage(), List.of()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> noSuchElement(NoSuchElementException e) {
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> faltaUnParametro(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(
                HttpStatus.BAD_REQUEST,
                "Falta el parámetro '%s'.".formatted(e.getParameterName()),
                List.of("Se esperaba un %s en la consulta.".formatted(e.getParameterType()))));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> faltaElArchivo(MissingServletRequestPartException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(
                HttpStatus.BAD_REQUEST,
                "Falta el archivo '%s' en la carga.".formatted(e.getRequestPartName()),
                List.of("La petición tiene que ser multipart e incluir esa parte.")));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> cargaMalArmada(MultipartException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(
                HttpStatus.BAD_REQUEST,
                "La carga tiene que enviar el archivo como multipart.",
                List.of("Falta el archivo, o la petición se mandó como JSON.")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroConFormatoInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(
                HttpStatus.BAD_REQUEST,
                "El valor de '%s' no tiene el formato esperado.".formatted(e.getName()),
                List.of("Los períodos van como aaaa-mm y las fechas como aaaa-mm-dd.")));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> credenciales(CredencialesInvalidasException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.de(HttpStatus.UNAUTHORIZED, e.getMessage(), List.of()));
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccesoDenegadoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.de(HttpStatus.FORBIDDEN, e.getMessage(), List.of()));
    }
}
