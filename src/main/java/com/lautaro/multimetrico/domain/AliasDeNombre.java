package com.lautaro.multimetrico.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "alias_de_nombre")
public class AliasDeNombre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 200, unique = true)
    private String nombre;

    @Column(name = "legajo", nullable = false, length = 40)
    private String legajo;

    protected AliasDeNombre() {
    }

    public AliasDeNombre(String nombre, String legajo) {
        this.nombre = nombre;
        this.legajo = legajo;
    }

    public Long id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String legajo() {
        return legajo;
    }

    public void apuntarA(String otroLegajo) {
        this.legajo = otroLegajo;
    }
}
