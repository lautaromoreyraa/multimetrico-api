package com.lautaro.multimetrico.medicion;

import com.lautaro.multimetrico.medicion.domain.Medicion;
import java.time.YearMonth;
import java.util.List;

/**
 * El puerto de persistencia de las mediciones.
 *
 * <p>Solo agrega y lee. No hay {@code actualizar} ni {@code borrar} a propósito:
 * una medición es un hecho fechado, y corregir un número es cargar otra medición
 * del mismo corte, no pisar la anterior. Si mañana aparece un método para
 * modificar en el lugar, se pierde la historia que este módulo existe para
 * guardar.
 *
 * <p><b>Todas las lecturas devuelven la historia en orden de carga.</b> No es un
 * detalle de implementación: es parte del contrato, porque el desempate entre
 * dos mediciones del mismo corte lo gana la última cargada. Una implementación
 * JPA tiene que ordenar por una clave que respete ese orden —un id autoincremental
 * o un instante de alta—, no por fecha de corte.
 */
public interface MedicionRepository {

    /** Agrega el lote entero y lo devuelve tal como quedó guardado. */
    List<Medicion> guardar(List<Medicion> mediciones);

    /** Toda la historia del mes, de todos los asesores, en orden de carga. */
    List<Medicion> historia(YearMonth periodo);

    /** La historia del mes de un asesor, en orden de carga. */
    List<Medicion> historiaDe(YearMonth periodo, String legajo);
}
