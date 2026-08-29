package com.lautaro.multimetrico.exception;

import com.lautaro.multimetrico.dto.error.ErrorResponse;
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
 * pliego del mes tiene que poder arreglarlo de una pasada.
 *
 * <p>El otro que vale la pena mirar es {@link PlanillaRechazadaException}, que
 * también sale 422 y con la misma idea: todos los desajustes juntos, para poder
 * corregir el perfil sin subir el archivo cinco veces.
 *
 * <p>Está en {@code exception} y no en un paquete de un módulo. Antes vivía en
 * {@code pliego.web} por razones históricas y era engañoso: atiende los errores
 * de los cinco controladores, no los del pliego.
 */
@RestControllerAdvice
public class ManejadorGlobalDeErrores {

    @ExceptionHandler(PliegoInvalidoException.class)
    public ResponseEntity<ErrorResponse> pliegoInvalido(PliegoInvalidoException e) {
        return ResponseEntity.unprocessableEntity().body(ErrorResponse.conViolaciones(
                HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e.violaciones()));
    }

    /**
     * El archivo no coincidía con el perfil: 422 y no 400.
     *
     * <p>La petición estaba bien formada —un multipart con un archivo y un perfil
     * que existe—; lo que no se pudo procesar es el contenido. Es la misma
     * distinción que hace el 422 del pliego.
     */
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

    /**
     * La red de contención del 404.
     *
     * <p>Lo que el sistema lanza a propósito es {@link RecursoNoEncontradoException};
     * este handler queda para el {@code Optional.orElseThrow()} sin argumentos que
     * se escape por algún lado. No se saca porque quitarlo convertiría ese caso en
     * un 500 sin mensaje.
     */
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

    /**
     * El login que no coincide.
     *
     * <p>El mensaje viene ya unificado desde el servicio: no dice si falló el
     * legajo o la contraseña, porque distinguirlos convertiría al login en una
     * forma de averiguar qué legajos existen.
     */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> credenciales(CredencialesInvalidasException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.de(HttpStatus.UNAUTHORIZED, e.getMessage(), List.of()));
    }

    /**
     * El dato existe pero no es de quien lo pide.
     *
     * <p>403 y no 404: ver {@link AccesoDenegadoException}. El mensaje dice de
     * quién es la fila, que es lo que le sirve a quien se equivocó de legajo, y
     * no revela ningún número.
     */
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccesoDenegadoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.de(HttpStatus.FORBIDDEN, e.getMessage(), List.of()));
    }
}
