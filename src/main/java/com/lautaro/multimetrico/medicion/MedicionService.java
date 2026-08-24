package com.lautaro.multimetrico.medicion;

import com.lautaro.multimetrico.medicion.domain.Medicion;
import com.lautaro.multimetrico.medicion.domain.MedicionesVigentes;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MedicionService {

    private final MedicionRepository repositorio;

    public MedicionService(MedicionRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Da de alta un lote de mediciones.
     *
     * <p>Es siempre un alta, nunca una actualización: si el mismo asesor ya
     * tenía TMO cargado, esta carga se suma a la historia y pasa a ser la
     * vigente si su corte es igual o posterior. Volver a subir el archivo de la
     * semana pasada no borra el de esta.
     *
     * <p>No valida que los legajos existan en el padrón, y es deliberado: quien
     * conoce los nombres del archivo de origen y sabe traducirlos a legajo es el
     * módulo de importación. Una medición cuyo legajo no está en el padrón del
     * período queda guardada pero no aparece en ningún multimétrico, porque el
     * multimétrico se arma recorriendo el equipo. Ver la nota de deuda conocida.
     */
    public List<Medicion> registrar(List<Medicion> mediciones) {
        if (mediciones == null || mediciones.isEmpty()) {
            throw new IllegalArgumentException("El lote no trae ninguna medición");
        }
        return repositorio.guardar(mediciones);
    }

    /**
     * La historia del mes, con dos filtros opcionales.
     *
     * <p>{@code legajo} y {@code metrica} en {@code null} significan "sin
     * filtrar". Sale en orden de carga —no ordenada por fecha de corte— porque
     * es el orden en el que se decide cuál vale, y verlo tal cual es lo que
     * permite reconstruir qué pasó.
     */
    public List<Medicion> historia(YearMonth periodo, String legajo, Metrica metrica) {
        List<Medicion> historia = legajo == null || legajo.isBlank()
                ? repositorio.historia(periodo)
                : repositorio.historiaDe(periodo, legajo);

        return metrica == null
                ? historia
                : historia.stream().filter(medicion -> medicion.metrica() == metrica).toList();
    }

    /** Lo que vale hoy cada métrica de un asesor. */
    public MedicionesVigentes vigentesDe(YearMonth periodo, String legajo) {
        return MedicionesVigentes.de(repositorio.historiaDe(periodo, legajo));
    }

    /**
     * Lo mismo para toda la operación, en una sola pasada por la historia.
     *
     * <p>El multimétrico de un equipo necesita esto para cada asesor. Pedirlo de
     * a uno recorrería la historia entera una vez por persona; acá se recorre una
     * sola vez y se agrupa. Con base de datos esto es un {@code group by}.
     *
     * <p>La clave es el legajo en mayúsculas: los archivos de origen escriben el
     * mismo legajo de las dos formas y el padrón ya los compara sin distinguir.
     */
    public Map<String, MedicionesVigentes> vigentesDelPeriodo(YearMonth periodo) {
        Map<String, List<Medicion>> porLegajo = new LinkedHashMap<>();
        for (Medicion medicion : repositorio.historia(periodo)) {
            porLegajo.computeIfAbsent(clave(medicion.legajo()), legajo -> new java.util.ArrayList<>())
                    .add(medicion);
        }

        Map<String, MedicionesVigentes> vigentes = new LinkedHashMap<>();
        porLegajo.forEach((legajo, historia) -> vigentes.put(legajo, MedicionesVigentes.de(historia)));
        return vigentes;
    }

    public static String clave(String legajo) {
        return legajo.trim().toUpperCase(Locale.ROOT);
    }
}
