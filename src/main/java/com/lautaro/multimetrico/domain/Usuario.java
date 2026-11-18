package com.lautaro.multimetrico.domain;

import com.lautaro.multimetrico.enumeration.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legajo", nullable = false, length = 40)
    private String legajo;

    @Column(name = "legajo_clave", nullable = false, length = 40)
    private String legajoClave;

    @Column(name = "nombre", nullable = false, length = 160)
    private String nombre;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    @Column(name = "lider", length = 160)
    private String lider;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected Usuario() {
    }

    public Usuario(String legajo, String nombre, String passwordHasheada, Rol rol, String lider) {
        this.legajo = legajo;
        this.legajoClave = clave(legajo);
        this.nombre = nombre;
        this.password = passwordHasheada;
        this.rol = rol;
        this.lider = lider;
        this.activo = true;
        this.creadoEn = Instant.now();
    }

    public static String clave(String legajo) {
        return legajo == null ? null : legajo.trim().toUpperCase(Locale.ROOT);
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

    public String password() {
        return password;
    }

    public Rol rol() {
        return rol;
    }

    public String lider() {
        return lider;
    }

    public boolean activo() {
        return activo;
    }

    public Instant creadoEn() {
        return creadoEn;
    }

    public void cambiarPassword(String passwordHasheada) {
        this.password = passwordHasheada;
    }

    public void desactivar() {
        this.activo = false;
    }
}
