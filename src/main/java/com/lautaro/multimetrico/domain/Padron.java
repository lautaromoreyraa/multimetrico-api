package com.lautaro.multimetrico.domain;

import com.lautaro.multimetrico.util.NombreNormalizado;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Entity
@Table(name = "padron")
public class Padron {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "periodo", nullable = false, length = 7, unique = true)
    private YearMonth periodo;

    @OneToMany(mappedBy = "padron", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<Asesor> asesores = new ArrayList<>();

    protected Padron() {
    }

    public Padron(YearMonth periodo, List<Asesor> asesores) {
        if (periodo == null) {
            throw new IllegalArgumentException("El padrón necesita un período");
        }
        if (asesores == null || asesores.isEmpty()) {
            throw new IllegalArgumentException("El padrón no tiene asesores");
        }
        this.periodo = periodo;

        Map<String, Asesor> porLegajo = new LinkedHashMap<>();
        int orden = 0;
        for (Asesor asesor : asesores) {
            Asesor previo = porLegajo.putIfAbsent(asesor.legajo(), asesor);
            if (previo != null) {
                throw new IllegalArgumentException(
                        "El legajo %s aparece dos veces en el padrón: %s y %s"
                                .formatted(asesor.legajo(), previo.nombre(), asesor.nombre()));
            }
            asesor.colgarDe(this, orden++);
            this.asesores.add(asesor);
        }
    }

    public Long id() {
        return id;
    }

    public YearMonth periodo() {
        return periodo;
    }

    public List<Asesor> asesores() {
        return List.copyOf(asesores);
    }

    public Optional<Asesor> porLegajo(String legajo) {
        return asesores.stream()
                .filter(asesor -> asesor.legajo().equalsIgnoreCase(legajo.trim()))
                .findFirst();
    }

    public Asesor agregar(Asesor nuevo) {
        porLegajo(nuevo.legajo()).ifPresent(previo -> {
            throw new IllegalArgumentException(
                    "El legajo %s ya está en el padrón de %s: %s"
                            .formatted(nuevo.legajo(), periodo, previo.nombre()));
        });

        nuevo.colgarDe(this, asesores.size());
        asesores.add(nuevo);
        return nuevo;
    }

    public Asesor corregir(String legajo, String nombre, String lider) {
        Asesor asesor = porLegajo(legajo).orElseThrow(() -> new java.util.NoSuchElementException(
                "El legajo %s no está en el padrón de %s".formatted(legajo, periodo)));

        asesor.corregir(nombre, lider);
        return asesor;
    }

    public Asesor quitar(String legajo) {
        Asesor asesor = porLegajo(legajo).orElseThrow(() -> new java.util.NoSuchElementException(
                "El legajo %s no está en el padrón de %s".formatted(legajo, periodo)));

        asesores.remove(asesor);
        return asesor;
    }

    public List<Asesor> equipoDe(String lider) {
        NombreNormalizado buscado = NombreNormalizado.de(lider);
        return asesores.stream()
                .filter(asesor -> asesor.liderNormalizado().equals(buscado))
                .sorted(Comparator.comparing(Asesor::nombre))
                .toList();
    }

    public List<String> lideres() {
        return asesores.stream()
                .map(Asesor::lider)
                .distinct()
                .sorted()
                .toList();
    }

    public int tamano() {
        return asesores.size();
    }
}
