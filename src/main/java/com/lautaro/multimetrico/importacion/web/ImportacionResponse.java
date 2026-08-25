package com.lautaro.multimetrico.importacion.web;

import com.lautaro.multimetrico.importacion.domain.Desajuste;
import com.lautaro.multimetrico.importacion.domain.DiscrepanciaDeLider;
import com.lautaro.multimetrico.importacion.domain.FilaPendiente;
import com.lautaro.multimetrico.importacion.domain.FilaRechazada;
import com.lautaro.multimetrico.importacion.domain.Importacion;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * El informe de una importación.
 *
 * <p>Las cuentas van arriba de todo y con {@code cuadra} explícito. La pregunta
 * que alguien se hace al subir un archivo no es "¿funcionó?" sino "¿entró todo?",
 * y esa se contesta con cuatro números que tienen que sumar.
 */
public record ImportacionResponse(
        String id,
        String perfil,
        Metrica metrica,
        YearMonth periodo,
        LocalDate fechaDeCorte,
        Importacion.Estado estado,
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
            List<CandidatoResponse> candidatos) {
    }

    public record CandidatoResponse(String legajo, String nombre) {
    }

    public record DiscrepanciaResponse(int fila, String asesor, String legajo,
            String liderDelArchivo, String liderDelPadron) {
    }

    public static ImportacionResponse de(Importacion importacion) {
        var informe = importacion.informe();

        return new ImportacionResponse(
                importacion.id(),
                importacion.perfil(),
                importacion.metrica(),
                importacion.periodo(),
                importacion.fechaDeCorte(),
                importacion.estado(),
                informe.filasLeidas(),
                informe.importadas(),
                informe.rechazadas().size(),
                informe.pendientes().size(),
                informe.cuadra(),
                informe.rechazadas().stream().map(ImportacionResponse::rechazo).toList(),
                informe.pendientes().stream().map(ImportacionResponse::pendiente).toList(),
                informe.discrepanciasDeLider().stream().map(ImportacionResponse::discrepancia).toList(),
                importacion.desajustes().stream().map(Desajuste::toString).toList());
    }

    private static RechazoResponse rechazo(FilaRechazada fila) {
        return new RechazoResponse(fila.numeroDeFila(), fila.textoAsesor(), fila.textoValor(), fila.explicacion());
    }

    private static PendienteResponse pendiente(FilaPendiente fila) {
        return new PendienteResponse(
                fila.numeroDeFila(),
                fila.textoAsesor(),
                fila.valor() == null ? null : fila.valor().toPlainString(),
                fila.explicacion(),
                fila.candidatos().stream()
                        .map(c -> new CandidatoResponse(c.legajo(), c.nombre()))
                        .toList());
    }

    private static DiscrepanciaResponse discrepancia(DiscrepanciaDeLider d) {
        return new DiscrepanciaResponse(d.numeroDeFila(), d.textoAsesor(), d.legajo(),
                d.liderDelArchivo(), d.liderDelPadron());
    }
}
