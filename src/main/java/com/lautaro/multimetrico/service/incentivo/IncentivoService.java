package com.lautaro.multimetrico.service.incentivo;

import com.lautaro.multimetrico.dto.incentivo.CargaDeIncentivos;
import com.lautaro.multimetrico.dto.incentivo.IncentivosResponse;
import com.lautaro.multimetrico.dto.perfil.PerfilDeIncentivos;
import java.time.YearMonth;

/**
 * Los importes que cobra cada asesor, tal como llegan de la empresa.
 *
 * <p>Dos operaciones y nada más: cargar el archivo del mes y mirar el resultado.
 * No hay ningún cálculo — ver {@code Incentivo}.
 */
public interface IncentivoService {

    /**
     * Lee el archivo, resuelve los nombres contra el padrón y guarda los importes.
     *
     * <p>Devuelve qué pasó con cada fila. Ninguna se pierde en silencio: la que no
     * se puede atribuir a un legajo sale como rechazada con su número de fila y
     * el motivo.
     */
    CargaDeIncentivos cargar(YearMonth periodo, PerfilDeIncentivos perfil, byte[] archivo, String origen);

    /**
     * Los incentivos del equipo de un líder; sin {@code lider}, la operación
     * entera.
     *
     * <p>Recorre del padrón hacia los incentivos: un asesor sin importe aparece
     * igual, con el importe vacío.
     */
    IncentivosResponse delEquipo(YearMonth periodo, String lider);
}
