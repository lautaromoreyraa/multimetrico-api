package com.lautaro.multimetrico.domain;

import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.util.DecimalUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

@Entity
@Table(name = "medicion")
public class Medicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legajo", nullable = false, length = 40)
    private String legajo;

    @Column(name = "legajo_clave", nullable = false, length = 40)
    private String legajoClave;

    @Enumerated(EnumType.STRING)
    @Column(name = "metrica", nullable = false, length = 40)
    private Metrica metrica;

    @Column(name = "periodo", nullable = false, length = 7)
    private YearMonth periodo;

    @Column(name = "valor", nullable = false, precision = 18, scale = 6)
    private BigDecimal valor;

    @Column(name = "fecha_de_corte", nullable = false)
    private LocalDate fechaDeCorte;

    @Column(name = "origen_importacion", length = 36)
    private String origenImportacion;

    protected Medicion() {
    }

    public Medicion(String legajo, Metrica metrica, YearMonth periodo, BigDecimal valor,
            LocalDate fechaDeCorte, String origenImportacion) {

        if (legajo == null || legajo.isBlank()) {
            throw new IllegalArgumentException("La medición necesita un legajo");
        }
        if (metrica == null) {
            throw new IllegalArgumentException("La medición de %s necesita una métrica".formatted(legajo));
        }
        if (periodo == null) {
            throw new IllegalArgumentException(
                    "La medición de %s en %s necesita un período".formatted(legajo, metrica));
        }
        if (valor == null) {
            throw new IllegalArgumentException(
                    "La medición de %s en %s necesita un valor. Una métrica sin dato no se carga en cero: "
                            .formatted(legajo, metrica)
                            + "se deja faltante.");
        }
        if (fechaDeCorte == null) {
            throw new IllegalArgumentException(
                    ("La medición de %s en %s necesita una fecha de corte. Sin ella no se sabe cuál de dos "
                            + "cargas es la vigente ni a qué día corresponde el número.").formatted(legajo, metrica));
        }
        this.legajo = legajo.trim();
        this.legajoClave = clave(legajo);
        this.metrica = metrica;
        this.periodo = periodo;
        this.valor = valor;
        this.fechaDeCorte = fechaDeCorte;
        this.origenImportacion = origenImportacion;
    }

    public static String clave(String legajo) {
        return legajo.trim().toUpperCase(Locale.ROOT);
    }

    public Long id() {
        return id;
    }

    public String legajo() {
        return legajo;
    }

    public String legajoClave() {
        return legajoClave;
    }

    public Metrica metrica() {
        return metrica;
    }

    public YearMonth periodo() {
        return periodo;
    }

    public BigDecimal valor() {
        return DecimalUtil.comoEntro(valor);
    }

    public LocalDate fechaDeCorte() {
        return fechaDeCorte;
    }

    public String origenImportacion() {
        return origenImportacion;
    }

    public boolean desplazaA(Medicion otra) {
        return !fechaDeCorte.isBefore(otra.fechaDeCorte());
    }

    public boolean esDe(String otroLegajo) {
        return legajo.equalsIgnoreCase(otroLegajo == null ? "" : otroLegajo.trim());
    }
}
