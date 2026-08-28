package com.lautaro.multimetrico.dto.importacion;

import java.util.List;

/**
 * Qué pasó con cada fila del archivo.
 *
 * <h2>La propiedad que define el módulo</h2>
 *
 * <p><b>{@code importadas + rechazadas + pendientes == filasLeidas}.</b> Cada
 * fila termina en exactamente uno de los tres estados. No hay una cuarta bolsa,
 * ni un "se ignoró", ni filas que se caen entre dos {@code if}.
 *
 * <p>Es el defecto central de la planilla que este proyecto reemplaza: los
 * archivos empalman por nombre, lo que no coincide no aparece, y el total sale
 * igual —más chico— sin que nada avise. Una importación que no cuadra no es un
 * informe incompleto: es el mismo error otra vez.
 *
 * <p>Por eso la cuenta se verifica <b>acá adentro</b> y no solo en un test, aun
 * siendo esto un DTO. Si algún día alguien agrega un camino que se olvida de
 * anotar la fila, el informe no se construye y la importación falla con el error
 * a la vista. Es preferible por lejos a publicar un informe que miente: un
 * informe que miente se lee como si fuera cierto. Un record puede validar un
 * invariante sin dejar de ser un record; lo que se movió a la capa de servicio es
 * el <em>cálculo</em>, no las condiciones que este objeto promete cumplir.
 *
 * <p>{@code discrepanciasDeLider} no participa de la cuenta. Son filas que
 * <em>sí</em> se importaron, anotadas aparte. Ver {@link DiscrepanciaDeLider}.
 */
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

    /** El informe de una importación que todavía no leyó nada, o que se rechazó entera. */
    public static Informe vacio() {
        return VACIO;
    }

    /**
     * Verdadero siempre: si no cuadrara, este objeto no existiría.
     *
     * <p>Se expone igual porque viaja en la respuesta de la API. Que el informe
     * diga en voz alta que cuadra es parte de lo que el módulo promete, y le
     * ahorra a quien consume la API tener que sumar tres números para confiar.
     */
    public boolean cuadra() {
        return importadas + rechazadas.size() + pendientes.size() == filasLeidas;
    }

    /**
     * El mismo informe con un pendiente resuelto: esas filas pasan a importadas.
     *
     * <p>La cuenta se mantiene porque las filas se mueven de una bolsa a la otra,
     * no se agregan. Es lo que hace {@code ImportacionServiceImpl} cuando una
     * persona confirma a qué legajo corresponde un nombre.
     */
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
