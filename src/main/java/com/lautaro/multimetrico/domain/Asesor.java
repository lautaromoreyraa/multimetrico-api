package com.lautaro.multimetrico.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "asesor")
public class Asesor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "padron_id", nullable = false)
    private Padron padron;

    @Column(name = "legajo", nullable = false, length = 40)
    private String legajo;

    @Column(name = "nombre", nullable = false, length = 160)
    private String nombre;

    @Column(name = "lider", nullable = false, length = 160)
    private String lider;

    @Column(name = "orden", nullable = false)
    private int orden;

    protected Asesor() {
    }

    public Asesor(String legajo, String nombre, String lider) {
        if (legajo == null || legajo.isBlank()) {
            throw new IllegalArgumentException("El asesor necesita un legajo");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El asesor %s necesita un nombre".formatted(legajo));
        }
        if (lider == null || lider.isBlank()) {
            throw new IllegalArgumentException("El asesor %s necesita un líder".formatted(legajo));
        }
        this.legajo = legajo.trim();
        this.nombre = nombre;
        this.lider = lider;
    }

    public void corregir(String nombre, String lider) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El asesor %s necesita un nombre".formatted(legajo));
        }
        if (lider == null || lider.isBlank()) {
            throw new IllegalArgumentException("El asesor %s necesita un líder".formatted(legajo));
        }
        this.nombre = nombre.trim();
        this.lider = lider.trim();
    }

    void colgarDe(Padron padron, int orden) {
        this.padron = padron;
        this.orden = orden;
    }

    public Long id() {
        return id;
    }

    public String legajo() {
        return legajo;
    }

    public String nombre() {
        return nombre;
    }

    public String lider() {
        return lider;
    }

    public int orden() {
        return orden;
    }

    public NombreNormalizado nombreNormalizado() {
        return NombreNormalizado.de(nombre);
    }

    public NombreNormalizado liderNormalizado() {
        return NombreNormalizado.de(lider);
    }
}
