package com.lautaro.multimetrico.importacion.web;

import com.lautaro.multimetrico.importacion.ImportacionService;
import com.lautaro.multimetrico.importacion.domain.PerfilDeImportacion;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ImportacionController {

    private final ImportacionService servicio;

    public ImportacionController(ImportacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/api/perfiles")
    public ResponseEntity<PerfilDeImportacion> definir(@Valid @RequestBody PerfilRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.definirPerfil(request.aDominio()));
    }

    @GetMapping("/api/perfiles")
    public List<PerfilDeImportacion> perfiles() {
        return servicio.perfiles();
    }

    /**
     * Devuelve las columnas del archivo con valores de ejemplo.
     *
     * <p>Es el arranque cuando llega una fuente nueva: el sistema no adivina qué
     * columna es cuál, muestra lo que hay y una persona arma el perfil mirándolo.
     */
    @PostMapping("/api/perfiles/inspeccion")
    public Map<String, Object> inspeccionar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(required = false) String hoja,
            @RequestParam(defaultValue = "1") int filaEncabezados) throws IOException {

        byte[] bytes = archivo.getBytes();
        List<List<String>> columnas = servicio.inspeccionar(bytes, hoja, filaEncabezados);

        List<Map<String, Object>> descripcion = columnas.stream()
                .map(columna -> {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("encabezado", columna.getFirst());
                    fila.put("ejemplos", columna.subList(1, columna.size()));
                    return fila;
                })
                .toList();

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("hojas", servicio.hojasDe(bytes));
        respuesta.put("filaEncabezados", filaEncabezados);
        respuesta.put("columnas", descripcion);
        return respuesta;
    }

    /**
     * Recibe el archivo y responde con el id.
     *
     * <p>Devuelve {@code 202 Accepted} y no {@code 201}: cuando la respuesta sale,
     * la importación todavía se está procesando. El estado se consulta después.
     */
    @PostMapping("/api/importaciones")
    public ResponseEntity<ImportacionResponse> importar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam String perfil,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate corte) throws IOException {

        var recibida = servicio.recibir(perfil, periodo, corte, archivo.getBytes());
        return ResponseEntity.accepted().body(ImportacionResponse.de(recibida));
    }

    @GetMapping("/api/importaciones/{id}")
    public ImportacionResponse ver(@PathVariable String id) {
        return ImportacionResponse.de(servicio.buscar(id));
    }

    @GetMapping("/api/importaciones")
    public List<ImportacionResponse> historial() {
        return servicio.historial().stream().map(ImportacionResponse::de).toList();
    }

    /**
     * Asigna a mano una fila que quedó pendiente.
     *
     * <p>Vale para esta fila de este archivo, y no crea un alias global: un nombre
     * cortado que apunta a dos personas puede ser una este mes y la otra el
     * siguiente.
     */
    @PostMapping("/api/importaciones/{id}/pendientes/{fila}")
    public ImportacionResponse resolver(
            @PathVariable String id,
            @PathVariable int fila,
            @RequestParam String legajo) {
        return ImportacionResponse.de(servicio.resolverPendiente(id, fila, legajo));
    }
}
