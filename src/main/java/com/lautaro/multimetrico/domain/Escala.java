package com.lautaro.multimetrico.domain;

import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.util.DecimalUtil;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "escala")
public class Escala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pliego_id", nullable = false)
    private Pliego pliego;

    @Enumerated(EnumType.STRING)
    @Column(name = "metrica", nullable = false, length = 40)
    private Metrica metrica;

    @Column(name = "peso", nullable = false, precision = 18, scale = 6)
    private BigDecimal peso;

    @Column(name = "orden", nullable = false)
    private int orden;

    @OneToMany(mappedBy = "escala", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private Set<Banda> bandas = new LinkedHashSet<>();

    protected Escala() {
    }

    public Escala(Metrica metrica, BigDecimal peso, List<Banda> bandas) {
        if (metrica == null) {
            throw new IllegalArgumentException("La escala necesita una métrica");
        }
        if (peso == null) {
            throw new IllegalArgumentException("La escala de %s necesita un peso".formatted(metrica));
        }
        if (bandas == null || bandas.isEmpty()) {
            throw new IllegalArgumentException("La escala de %s no tiene bandas".formatted(metrica));
        }
        this.metrica = metrica;
        this.peso = peso;

        int posicion = 0;
        for (Banda banda : bandas) {
            banda.colgarDe(this, posicion++);
            this.bandas.add(banda);
        }
    }

    void colgarDe(Pliego pliego, int orden) {
        this.pliego = pliego;
        this.orden = orden;
    }

    public Long id() {
        return id;
    }

    public Metrica metrica() {
        return metrica;
    }

    public BigDecimal peso() {
        return DecimalUtil.comoEntro(peso);
    }

    public int orden() {
        return orden;
    }

    public List<Banda> bandas() {
        return List.copyOf(bandas);
    }

    public List<Banda> ordenadas() {
        return bandas.stream()
                .sorted(Comparator.comparing(Banda::desde, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
    }

    public Optional<Banda> bandaPara(BigDecimal valor) {
        return bandas.stream().filter(banda -> banda.contiene(valor)).findFirst();
    }

    public BigDecimal mejorResultado() {
        return bandas.stream()
                .map(Banda::resultado)
                .max(BigDecimal::compareTo)
                .orElseThrow();
    }
}
