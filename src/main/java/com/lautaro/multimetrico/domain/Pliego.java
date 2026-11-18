package com.lautaro.multimetrico.domain;

import com.lautaro.multimetrico.enumeration.Metrica;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "pliego")
public class Pliego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "periodo", nullable = false, length = 7)
    private YearMonth periodo;

    @Column(name = "campania", nullable = false, length = 120)
    private String campania;

    @Column(name = "campania_clave", nullable = false, length = 120)
    private String campaniaClave;

    @OneToMany(mappedBy = "pliego", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private Set<Escala> escalas = new LinkedHashSet<>();

    protected Pliego() {
    }

    public Pliego(YearMonth periodo, String campania, List<Escala> escalas) {
        if (periodo == null) {
            throw new IllegalArgumentException("El pliego necesita un período");
        }
        if (campania == null || campania.isBlank()) {
            throw new IllegalArgumentException("El pliego necesita una campaña");
        }
        if (escalas == null || escalas.isEmpty()) {
            throw new IllegalArgumentException("El pliego no tiene escalas");
        }
        this.periodo = periodo;
        this.campania = campania;
        this.campaniaClave = clave(campania);

        int orden = 0;
        for (Escala escala : escalas) {
            escala.colgarDe(this, orden++);
            this.escalas.add(escala);
        }
    }

    public static String clave(String campania) {
        return campania.trim().toUpperCase(Locale.ROOT);
    }

    public Long id() {
        return id;
    }

    public YearMonth periodo() {
        return periodo;
    }

    public String campania() {
        return campania;
    }

    public String campaniaClave() {
        return campaniaClave;
    }

    public List<Escala> escalas() {
        return List.copyOf(escalas);
    }

    public Optional<Escala> escalaDe(Metrica metrica) {
        return escalas.stream().filter(escala -> escala.metrica() == metrica).findFirst();
    }

    public BigDecimal alcanceMaximo() {
        return escalas.stream()
                .map(Escala::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
