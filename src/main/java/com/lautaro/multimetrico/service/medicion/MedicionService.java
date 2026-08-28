package com.lautaro.multimetrico.service.medicion;

import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.dto.medicion.MedicionesVigentes;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * La historia de mediciones y el cálculo de cuál vale hoy.
 *
 * <p>Solo agrega y lee. No hay {@code actualizar} ni {@code borrar} a propósito:
 * una medición es un hecho fechado, y corregir un número es cargar otra medición
 * del mismo corte, no pisar la anterior. Si mañana aparece un método para
 * modificar en el lugar, se pierde la historia que este módulo existe para
 * guardar.
 */
public interface MedicionService {

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
     * multimétrico se arma recorriendo el equipo.
     */
    List<Medicion> registrar(List<Medicion> mediciones);

    /**
     * La historia del mes, con dos filtros opcionales.
     *
     * <p>{@code legajo} y {@code metrica} en {@code null} significan "sin
     * filtrar". Sale <b>en orden de carga</b> —no ordenada por fecha de corte—
     * porque es el orden en el que se decide cuál vale, y verlo tal cual es lo que
     * permite reconstruir qué pasó.
     */
    List<Medicion> historia(YearMonth periodo, String legajo, Metrica metrica);

    /**
     * Los ids de las mediciones que están valiendo dentro de una historia dada.
     *
     * <p>Se calcula <b>por asesor y por métrica</b>, no sobre la lista entera:
     * pedir la historia de un mes sin filtrar trae a toda la operación, y ahí hay
     * una vigente por cada persona.
     *
     * <p>Devuelve ids y no las mediciones para que quien las marque las compare
     * por identidad de fila y no por valor: dos cargas iguales del mismo corte
     * tienen todos los campos idénticos y solo se distinguen por el id.
     */
    Set<Long> vigentesEn(List<Medicion> historia);

    /** Lo que vale hoy cada métrica de un asesor. */
    MedicionesVigentes vigentesDe(YearMonth periodo, String legajo);

    /**
     * Lo mismo para toda la operación, en una sola pasada por la historia.
     *
     * <p>El multimétrico de un equipo necesita esto para cada asesor. Pedirlo de
     * a uno recorrería la historia entera una vez por persona; acá se recorre una
     * sola vez y se agrupa.
     *
     * <p>La clave del mapa es el legajo en mayúsculas: los archivos de origen
     * escriben el mismo legajo de las dos formas y el padrón ya los compara sin
     * distinguir.
     */
    Map<String, MedicionesVigentes> vigentesDelPeriodo(YearMonth periodo);
}
