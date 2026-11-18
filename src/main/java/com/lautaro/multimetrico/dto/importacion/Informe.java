package com.lautaro.multimetrico.dto.importacion;

import java.util.List;

public record Informe(
        int filasLeidas,
        int importadas,
        List<FilaRechazada> rechazadas,
        List<FilaPendiente> pendientes,
        List<DiscrepanciaDeLider> discrepanciasDeLider) {

    private static final Informe VACIO = new Informe(0, 0, List.of(), List.of(), List.of());

    public Informe {
        rechazadas = List.copyOf(rechazadas == null ? List.of() : rechazadas);
        pendientes = List.copyOf(pendientes == null ? List.of() : pendientes);
        discrepanciasDeLider = List.copyOf(discrepanciasDeLider == null ? List.of() : discrepanciasDeLider);

        int contadas = importadas + rechazadas.size() + pendientes.size();
        if (contadas != filasLeidas) {
            throw new IllegalStateException(
                    ("El informe no cuadra: se leyeron %d filas y las anotadas son %d "
                            + "(%d importadas, %d rechazadas, %d pendientes). Cada fila tiene que terminar "
                            + "en exactamente uno de los tres estados.")
                            .formatted(filasLeidas, contadas, importadas, rechazadas.size(), pendientes.size()));
        }
    }

    public static Informe vacio() {
        return VACIO;
    }

    public boolean cuadra() {
        return importadas + rechazadas.size() + pendientes.size() == filasLeidas;
    }

    public Informe conPendientesResueltos(List<FilaPendiente> resueltos) {
        List<FilaPendiente> quedan = pendientes.stream()
                .filter(pendiente -> !resueltos.contains(pendiente))
                .toList();
        return new Informe(
                filasLeidas,
                importadas + (pendientes.size() - quedan.size()),
                rechazadas,
                quedan,
                discrepanciasDeLider);
    }
}
