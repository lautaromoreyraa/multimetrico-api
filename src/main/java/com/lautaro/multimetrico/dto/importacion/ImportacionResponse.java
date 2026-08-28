package com.lautaro.multimetrico.dto.importacion;

import com.lautaro.multimetrico.dto.padron.Candidato;
import com.lautaro.multimetrico.enumeration.EstadoDeImportacion;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * El informe de una importación.
 *
 * <p>Las cuentas van arriba de todo y con {@code cuadra} explícito. La pregunta
 * que alguien se hace al subir un archivo no es "¿funcionó?" sino "¿entró todo?",
 * y esa se contesta con cuatro números que tienen que sumar.
 *
 * <p>{@code RECHAZADA} no es "terminó con rechazos": es que el archivo no
 * coincidía con el perfil y no entró <b>nada</b>. Los contadores quedan en cero y
 * el motivo está en {@code desajustes}. Son dos situaciones con acciones
 * distintas y la pantalla las separa.
 */
public record ImportacionResponse(
        String id,
        String perfil,
        Metrica metrica,
        YearMonth periodo,
        LocalDate fechaDeCorte,
        EstadoDeImportacion estado,
        int filasLeidas,
        int importadas,
        int rechazadas,
        int pendientes,
        boolean cuadra,
        List<RechazoResponse> detalleRechazadas,
        List<PendienteResponse> detallePendientes,
        List<DiscrepanciaResponse> discrepanciasDeLider,
        List<String> desajustes) {

    public record RechazoResponse(int fila, String asesor, String valor, String motivo) {
    }

    public record PendienteResponse(int fila, String asesor, String valor, String motivo,
            List<Candidato> candidatos) {
    }

    public record DiscrepanciaResponse(int fila, String asesor, String legajo,
            String liderDelArchivo, String liderDelPadron) {
    }
}
