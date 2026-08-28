package com.lautaro.multimetrico.dto.medicion;

import com.lautaro.multimetrico.enumeration.Metrica;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Un lote de mediciones como llega por HTTP.
 *
 * <p>El período y la fecha de corte van <b>arriba</b>, una sola vez, porque así
 * es como llega el dato real: un archivo por métrica y por semana, con el corte
 * de esa semana, para toda la operación. Repetir la misma fecha en cada una de
 * las doscientas filas invita a que una salga distinta por un error de tipeo.
 *
 * <p>Aun así cada ítem puede traer su propia {@code fechaDeCorte} y pisar la del
 * lote. Es para el caso de subir de una vez dos archivos con cortes distintos,
 * que es exactamente lo que hace el módulo de importación cuando procesa varias
 * métricas juntas. Sin ese escape, el importador tendría que partir el lote y
 * perdería la atomicidad.
 */
public record MedicionesRequest(
        @NotNull(message = "el período es obligatorio (formato aaaa-mm)") YearMonth periodo,
        @NotNull(message = "la fecha de corte es obligatoria (formato aaaa-mm-dd)") LocalDate fechaDeCorte,
        String origenImportacion,
        @NotEmpty(message = "el lote necesita al menos una medición") @Valid List<ItemRequest> mediciones) {

    public record ItemRequest(
            @NotBlank(message = "el legajo es obligatorio") String legajo,
            @NotNull(message = "la métrica es obligatoria") Metrica metrica,
            @NotNull(message = "el valor es obligatorio; una métrica sin dato no se carga") BigDecimal valor,
            LocalDate fechaDeCorte) {
    }
}
