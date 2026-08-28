package com.lautaro.multimetrico.service.procesamiento;

import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.importacion.Informe;
import com.lautaro.multimetrico.dto.planilla.FilaLeida;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Corre el procesamiento de una importación fuera del hilo de la petición.
 *
 * <p><b>Es un servicio aparte y eso no es opcional.</b> {@code @Async} lo aplica
 * un proxy que envuelve al bean, así que una llamada de un método a otro <i>del
 * mismo objeto</i> no pasa por el proxy y se ejecuta como cualquier llamada
 * común: sincrónica, en el hilo de la petición. La anotación queda ahí, se lee
 * como si funcionara, y no hace nada.
 *
 * <p>Este proyecto lo tuvo escrito así y el síntoma no era un error sino un stack
 * trace donde el procesamiento colgaba de {@code recibir}. Separarlo en su propio
 * bean —que se inyecta y por lo tanto se llama a través del proxy— es la forma de
 * que el asincronismo sea real y no decorativo.
 */
public interface ProcesadorDeImportaciones {

    /**
     * Lee el archivo y deja la importación en su estado final.
     *
     * <p>El {@code interprete} es la parte que sabe de padrón y mediciones; vive
     * en {@code ImportacionService} y llega como función para que este servicio se
     * ocupe solo de la mecánica: leer, atajar los fracasos y guardar.
     *
     * @param archivo los bytes ya copiados. El {@code InputStream} del upload se
     *     cierra cuando la respuesta se va, y esto arranca después.
     */
    void procesar(String id, PerfilDeImportacion perfil, YearMonth periodo, LocalDate corte,
            byte[] archivo, BiFunction<String, List<FilaLeida>, Informe> interprete);
}
