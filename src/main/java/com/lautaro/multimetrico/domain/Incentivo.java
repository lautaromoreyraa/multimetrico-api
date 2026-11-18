package com.lautaro.multimetrico.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Locale;

@Entity
@Table(name = "incentivo")
public class Incentivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "periodo", nullable = false, length = 7)
    private String periodo;

    @Column(name = "legajo", nullable = false, length = 40)
    private String legajo;

    @Column(name = "legajo_clave", nullable = false, length = 40)
    private String legajoClave;

    @Column(name = "importe", nullable = false, precision = 18, scale = 2)
    private BigDecimal importe;

    @Column(name = "origen", length = 160)
    private String origen;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected Incentivo() {
    }

    public Incentivo(YearMonth periodo, String legajo, BigDecimal importe, String origen) {
        if (periodo == null) {
            throw new IllegalArgumentException("El incentivo necesita un período");
        }
        if (legajo == null || legajo.isBlank()) {
            throw new IllegalArgumentException("El incentivo necesita un legajo");
        }
        if (importe == null) {
            throw new IllegalArgumentException(
                    "El incentivo de %s necesita un importe. Un importe que no vino no es cero."
                            .formatted(legajo));
        }

        this.periodo = periodo.toString();
        this.legajo = legajo.trim();
        this.legajoClave = clave(legajo);
        this.importe = importe;
        this.origen = origen;
        this.creadoEn = Instant.now();
    }

    public static String clave(String legajo) {
        return legajo == null ? null : legajo.trim().toUpperCase(Locale.ROOT);
    }

    public Long id() {
        return id;
    }

    public YearMonth periodo() {
        return YearMonth.parse(periodo);
    }

    public String legajo() {
        return legajo;
    }

    public BigDecimal importe() {
        return importe;
    }

    public String origen() {
        return origen;
    }

    public Instant creadoEn() {
        return creadoEn;
    }
}
