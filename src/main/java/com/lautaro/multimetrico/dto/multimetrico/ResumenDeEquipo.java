package com.lautaro.multimetrico.dto.multimetrico;

import com.lautaro.multimetrico.enumeration.Metrica;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public record ResumenDeEquipo(
        int asesores,
        long completos,
        long conFaltantes,
        LocalDate actualizadoDesde,
        Map<Metrica, Set<LocalDate>> cortesPorMetrica) {
}
