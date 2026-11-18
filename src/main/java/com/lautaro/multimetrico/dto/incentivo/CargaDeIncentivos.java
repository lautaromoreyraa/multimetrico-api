package com.lautaro.multimetrico.dto.incentivo;

import java.time.YearMonth;
import java.util.List;

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
