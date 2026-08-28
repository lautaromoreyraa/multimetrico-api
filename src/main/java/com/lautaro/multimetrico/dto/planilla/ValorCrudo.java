package com.lautaro.multimetrico.dto.planilla;

import java.math.BigDecimal;

/**
 * Una celda tal como estaba en la planilla, antes de decidir qué significa.
 *
 * <p>Guarda las tres cosas que hacen falta para interpretarla y para poder
 * explicar después por qué se la rechazó:
 *
 * <ul>
 *   <li>{@code texto} — lo que muestra Excel. Es lo que va en el motivo del
 *       rechazo, porque es lo único que el supervisor puede reconocer cuando
 *       abra el archivo a buscar la fila.</li>
 *   <li>{@code numero} — el valor binario, <b>solo si la celda era numérica</b>.
 *       En {@code null} cuando la celda traía texto, que es la señal de que hay
 *       que parsear y de que esa columna viene mezclada.</li>
 *   <li>{@code formatoPorcentaje} — si la celda tenía formato de porcentaje.
 *       Excel guarda {@code 0.805} y muestra {@code 80,5%}: sin este dato el
 *       mismo número se puede leer cien veces más chico.</li>
 * </ul>
 *
 * <p>El número llega como {@link BigDecimal} construido desde el {@code double}
 * de la celda con {@link BigDecimal#valueOf(double)} y no con el constructor:
 * el constructor expande el binario entero y convierte un {@code 80.5} en
 * {@code 80.5000000000000284...}, que después cae del lado equivocado de un
 * borde de banda. Es el mismo cuidado que el resto del sistema tiene al comparar
 * con {@code compareTo}.
 */
public record ValorCrudo(String texto, BigDecimal numero, boolean formatoPorcentaje) {

    private static final ValorCrudo VACIO = new ValorCrudo("", null, false);

    public ValorCrudo {
        texto = texto == null ? "" : texto.trim();
    }

    /**
     * La celda que no estaba: fuera del rango de datos, o sin contenido.
     *
     * <p>Se llama {@code ninguno} y no {@code vacio} porque {@link #estaVacio()}
     * ya ocupa ese nombre para la pregunta. Una fábrica estática y un predicado
     * de instancia con el mismo nombre no compilan, y aunque compilaran, leer
     * {@code ValorCrudo.vacio()} al lado de {@code crudo.vacio()} es una forma
     * segura de confundir la celda con la pregunta sobre la celda.
     */
    public static ValorCrudo ninguno() {
        return VACIO;
    }

    public static ValorCrudo deTexto(String texto) {
        return new ValorCrudo(texto, null, false);
    }

    public static ValorCrudo deNumero(double valor, boolean formatoPorcentaje, String comoSeVe) {
        return new ValorCrudo(comoSeVe, BigDecimal.valueOf(valor), formatoPorcentaje);
    }

    public boolean estaVacio() {
        return numero == null && texto.isBlank();
    }
}
