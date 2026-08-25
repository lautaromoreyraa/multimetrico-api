package com.lautaro.multimetrico.importacion;

import com.lautaro.multimetrico.importacion.domain.Desajuste;
import com.lautaro.multimetrico.importacion.domain.FilaLeida;
import com.lautaro.multimetrico.importacion.domain.Importacion;
import com.lautaro.multimetrico.importacion.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.importacion.domain.PlanillaRechazada;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Corre el procesamiento de una importación fuera del hilo de la petición.
 *
 * <p><b>Es un bean aparte y eso no es opcional.</b> {@code @Async} lo aplica un
 * proxy que envuelve al bean, así que una llamada de un método a otro <i>del
 * mismo objeto</i> no pasa por el proxy y se ejecuta como cualquier llamada
 * común: sincrónica, en el hilo de la petición. La anotación queda ahí, se lee
 * como si funcionara, y no hace nada.
 *
 * <p>Este proyecto lo tuvo escrito así y el síntoma no era un error sino un stack
 * trace donde el procesamiento colgaba de {@code recibir}. Separar el
 * procesamiento en su propio bean —que se inyecta y por lo tanto se llama a
 * través del proxy— es la forma de que el asincronismo sea real y no decorativo.
 */
@Component
public class ProcesadorDeImportaciones {

    private final LectorDePlanilla lector;
    private final ImportacionRepository importaciones;

    public ProcesadorDeImportaciones(LectorDePlanilla lector, ImportacionRepository importaciones) {
        this.lector = lector;
        this.importaciones = importaciones;
    }

    /**
     * Lee el archivo y deja la importación en su estado final.
     *
     * <p>El {@code interprete} es la parte que sabe de padrón y mediciones; vive
     * en {@link ImportacionService} y llega como función para que este componente
     * se ocupe solo de la mecánica: leer, atajar los fracasos y guardar.
     */
    @Async("importaciones")
    public void procesar(String id, PerfilDeImportacion perfil, YearMonth periodo, LocalDate corte,
            byte[] archivo, BiFunction<String, List<FilaLeida>, com.lautaro.multimetrico.importacion.domain.Informe> interprete) {

        Importacion importacion = importaciones.porId(id).orElseThrow();
        try {
            List<FilaLeida> filas = lector.leer(new ByteArrayInputStream(archivo), perfil);
            importaciones.guardar(importacion.terminada(interprete.apply(id, filas)));
        } catch (PlanillaRechazada e) {
            importaciones.guardar(importacion.rechazada(e.desajustes()));
        } catch (NoSuchElementException e) {
            // El padrón del período no está cargado. No es culpa del archivo, así
            // que se informa como desajuste del contexto y no como filas rechazadas.
            importaciones.guardar(importacion.rechazada(List.of(Desajuste.padron(periodo, e.getMessage()))));
        }
    }
}
