package com.lautaro.multimetrico.dto.multimetrico;

import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record FilaDeMultimetrico(
        AsesorResponse asesor,
        List<CeldaDeMultimetrico> celdas,
        List<Metrica> faltantes,
        List<Metrica> fueraDeEscala,
        BigDecimal alcanceTotal,
        boolean completa,
        LocalDate corteMasViejo,
        LocalDate corteMasNuevo) {

    public FilaDeMultimetrico {
        celdas = List.copyOf(celdas);
        faltantes = List.copyOf(faltantes);
        fueraDeEscala = List.copyOf(fueraDeEscala);
    }

    public Map<Metrica, LocalDate> cortesPorMetrica() {
        Map<Metrica, LocalDate> cortes = new EnumMap<>(Metrica.class);
        celdas.forEach(celda -> cortes.put(celda.metrica(), celda.actualizadoAl()));
        return cortes;
    }
}
