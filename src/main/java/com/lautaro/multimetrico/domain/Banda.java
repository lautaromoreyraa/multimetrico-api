package com.lautaro.multimetrico.domain;

import com.lautaro.multimetrico.util.DecimalUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Un tramo del pliego: el rango de valores que paga un mismo alcance.
 *
 * <p><b>El intervalo es semiabierto: {@code [desde, hasta)}.</b> El límite de
 * abajo entra, el de arriba no, y el {@code hasta} de una banda es exactamente
 * el {@code desde} de la siguiente. Es la decisión que sostiene todo lo demás,
 * así que conviene decir por qué.
 *
 * <p>Los pliegos en planilla se escriben con los dos extremos incluidos y con
 * el número ya redondeado para mostrar: {@code 0–642}, después {@code 643–655};
 * {@code 79,5%–80,0%}, después {@code 80,1%–81,1%}. Eso deja dos agujeros que
 * no se ven hasta que muerden:
 *
 * <ul>
 *   <li><b>Huecos.</b> Entre el {@code 80,0%} de una banda y el {@code 80,1%}
 *       de la siguiente no hay nada. Un FCR de 80,04% se muestra como "80,0%"
 *       redondeado, pero el valor que se compara es el completo, y no cae en
 *       ninguna banda.</li>
 *   <li><b>Superposiciones.</b> Basta un dígito mal tipeado —una banda que
 *       termina en 681 y la siguiente que arranca en 680— para que un valor
 *       pertenezca a dos bandas a la vez. La planilla resuelve por la primera
 *       que encuentra y no avisa.</li>
 * </ul>
 *
 * <p>Con intervalos semiabiertos las dos cosas dejan de ser posibles por
 * construcción: si {@code hasta[n] == desde[n+1]}, no hay lugar donde meter un
 * hueco ni forma de que dos bandas se pisen. Lo que antes era un error silencioso
 * pasa a ser una condición que el validador puede exigir y nombrar.
 *
 * <p>Todo en {@link BigDecimal} y comparado con {@code compareTo}: los valores
 * caen justo sobre los bordes de banda todo el tiempo, que es exactamente donde
 * el punto flotante binario falla.
 */
@Entity
@Table(name = "banda")
public class Banda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escala_id", nullable = false)
    private Escala escala;

    /**
     * {@code null} es el extremo abierto: "sin mínimo".
     *
     * <p>No se reemplaza por un centinela ni por el mínimo del tipo: la banda
     * {@code [-∞, 50)} existe justamente para que un valor absurdo caiga en algún
     * lado, y un {@code -99999} guardado convertiría "sin piso" en "piso muy
     * bajo", que no es lo mismo cuando alguien lee la tabla.
     */
    @Column(name = "desde", precision = 18, scale = 6)
    private BigDecimal desde;

    /** {@code null} es "sin máximo": es lo que evita que un FCR de 100% se caiga de la escala. */
    @Column(name = "hasta", precision = 18, scale = 6)
    private BigDecimal hasta;

    @Column(name = "resultado", nullable = false, precision = 18, scale = 6)
    private BigDecimal resultado;

    /** El orden de carga, para que la tabla se lea como se escribió. */
    @Column(name = "orden", nullable = false)
    private int orden;

    protected Banda() {
    }

    public Banda(BigDecimal desde, BigDecimal hasta, BigDecimal resultado) {
        if (resultado == null) {
            throw new IllegalArgumentException("La banda necesita un resultado");
        }
        if (desde != null && hasta != null && desde.compareTo(hasta) >= 0) {
            throw new IllegalArgumentException(
                    "El desde (%s) tiene que ser menor que el hasta (%s)".formatted(desde, hasta));
        }
        this.desde = desde;
        this.hasta = hasta;
        this.resultado = resultado;
    }

    void colgarDe(Escala escala, int orden) {
        this.escala = escala;
        this.orden = orden;
    }

    public Long id() {
        return id;
    }

    /** Ver {@link DecimalUtil}: la base devuelve seis decimales y los mensajes se leen. */
    public BigDecimal desde() {
        return DecimalUtil.comoEntro(desde);
    }

    public BigDecimal hasta() {
        return DecimalUtil.comoEntro(hasta);
    }

    public BigDecimal resultado() {
        return DecimalUtil.comoEntro(resultado);
    }

    public int orden() {
        return orden;
    }

    /** {@code desde <= valor < hasta}, con los nulos valiendo por infinito. */
    public boolean contiene(BigDecimal valor) {
        boolean pasaElPiso = desde == null || valor.compareTo(desde) >= 0;
        boolean pasaElTecho = hasta == null || valor.compareTo(hasta) < 0;
        return pasaElPiso && pasaElTecho;
    }

    public boolean abiertaAbajo() {
        return desde == null;
    }

    public boolean abiertaArriba() {
        return hasta == null;
    }

    /** Para los mensajes del validador, que es lo que va a leer una persona. */
    public String rango() {
        String piso = desde == null ? "-∞" : desde().toPlainString();
        String techo = hasta == null ? "+∞" : hasta().toPlainString();
        return "[%s, %s)".formatted(piso, techo);
    }
}
