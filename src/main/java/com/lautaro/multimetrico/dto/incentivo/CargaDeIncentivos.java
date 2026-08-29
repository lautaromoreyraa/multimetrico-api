package com.lautaro.multimetrico.dto.incentivo;

import java.time.YearMonth;
import java.util.List;

/**
 * Qué pasó con cada fila del archivo de incentivos.
 *
 * <p><b>{@code importadas + rechazadas.size() == filasLeidas}.</b> Es la misma
 * propiedad que sostiene el módulo de importación y se verifica acá adentro por
 * la misma razón: el defecto central de la planilla que este proyecto reemplaza
 * es que las filas que no empalman por nombre desaparecen sin aviso, y el total
 * sale más chico sin que nada lo diga.
 *
 * <p>A diferencia de la importación de métricas, acá no hay bolsa de
 * "pendientes": un nombre ambiguo se rechaza con los candidatos escritos en el
 * motivo. Es más pobre que el flujo de resolución que tienen las métricas, y es a
 * propósito para esta primera versión — lo importante es que nada se pierda en
 * silencio, y una fila rechazada con su motivo cumple eso. Si el archivo de
 * incentivos empieza a traer nombres ambiguos seguido, ahí conviene el flujo
 * completo.
 */
public record CargaDeIncentivos(
        YearMonth periodo,
        String origen,
        int filasLeidas,
        int importadas,
        List<FilaSinCargar> rechazadas) {

    public CargaDeIncentivos {
        rechazadas = List.copyOf(rechazadas == null ? List.of() : rechazadas);

        int contadas = importadas + rechazadas.size();
        if (contadas != filasLeidas) {
            throw new IllegalStateException(
                    ("La carga no cuadra: se leyeron %d filas y las anotadas son %d "
                            + "(%d importadas, %d rechazadas). Cada fila tiene que terminar en "
                            + "exactamente uno de los dos estados.")
                            .formatted(filasLeidas, contadas, importadas, rechazadas.size()));
        }
    }

    public boolean cuadra() {
        return importadas + rechazadas.size() == filasLeidas;
    }
}
