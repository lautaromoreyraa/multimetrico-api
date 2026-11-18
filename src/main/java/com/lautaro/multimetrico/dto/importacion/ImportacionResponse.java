package com.lautaro.multimetrico.dto.importacion;

import com.lautaro.multimetrico.dto.padron.Candidato;
import com.lautaro.multimetrico.enumeration.EstadoDeImportacion;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

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
