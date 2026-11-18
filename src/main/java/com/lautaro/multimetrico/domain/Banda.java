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

@Entity
@Table(name = "banda")
public class Banda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escala_id", nullable = false)
    private Escala escala;

    @Column(name = "desde", precision = 18, scale = 6)
    private BigDecimal desde;

    @Column(name = "hasta", precision = 18, scale = 6)
    private BigDecimal hasta;

    @Column(name = "resultado", nullable = false, precision = 18, scale = 6)
    private BigDecimal resultado;

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

    public String rango() {
        String piso = desde == null ? "-∞" : desde().toPlainString();
        String techo = hasta == null ? "+∞" : hasta().toPlainString();
        return "[%s, %s)".formatted(piso, techo);
    }
}
