package com.lautaro.multimetrico.domain;

import com.lautaro.multimetrico.enumeration.EstadoDeImportacion;
import com.lautaro.multimetrico.enumeration.Metrica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "importacion")
@EntityListeners(AuditingEntityListener.class)
public class Importacion {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "perfil", nullable = false, length = 120)
    private String perfil;

    @Enumerated(EnumType.STRING)
    @Column(name = "metrica", nullable = false, length = 40)
    private Metrica metrica;

    @Column(name = "periodo", nullable = false, length = 7)
    private YearMonth periodo;

    @Column(name = "fecha_de_corte", nullable = false)
    private LocalDate fechaDeCorte;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoDeImportacion estado;

    @Column(name = "filas_leidas", nullable = false)
    private int filasLeidas;

    @Column(name = "importadas", nullable = false)
    private int importadas;

    @Column(name = "rechazadas_json")
    private String rechazadasJson;

    @Column(name = "pendientes_json")
    private String pendientesJson;

    @Column(name = "discrepancias_json")
    private String discrepanciasJson;

    @Column(name = "desajustes_json")
    private String desajustesJson;

    @CreatedDate
    @Column(name = "iniciada", nullable = false)
    private Instant iniciada;

    @Column(name = "terminada")
    private Instant terminada;

    protected Importacion() {
    }

    public Importacion(String id, String perfil, Metrica metrica, YearMonth periodo, LocalDate fechaDeCorte) {
        this.id = id;
        this.perfil = perfil;
        this.metrica = metrica;
        this.periodo = periodo;
        this.fechaDeCorte = fechaDeCorte;
        this.estado = EstadoDeImportacion.EN_CURSO;
        this.filasLeidas = 0;
        this.importadas = 0;
    }

    public void anotarInforme(int filasLeidas, int importadas, String rechazadasJson,
            String pendientesJson, String discrepanciasJson) {

        this.filasLeidas = filasLeidas;
        this.importadas = importadas;
        this.rechazadasJson = rechazadasJson;
        this.pendientesJson = pendientesJson;
        this.discrepanciasJson = discrepanciasJson;
    }

    public void terminar(int filasLeidas, int importadas, String rechazadasJson,
            String pendientesJson, String discrepanciasJson) {

        anotarInforme(filasLeidas, importadas, rechazadasJson, pendientesJson, discrepanciasJson);
        this.desajustesJson = null;
        this.estado = EstadoDeImportacion.TERMINADA;
        this.terminada = Instant.now();
    }

    public void rechazar(String desajustesJson) {
        anotarInforme(0, 0, null, null, null);
        this.desajustesJson = desajustesJson;
        this.estado = EstadoDeImportacion.RECHAZADA;
        this.terminada = Instant.now();
    }

    public String id() {
        return id;
    }

    public String perfil() {
        return perfil;
    }

    public Metrica metrica() {
        return metrica;
    }

    public YearMonth periodo() {
        return periodo;
    }

    public LocalDate fechaDeCorte() {
        return fechaDeCorte;
    }

    public EstadoDeImportacion estado() {
        return estado;
    }

    public int filasLeidas() {
        return filasLeidas;
    }

    public int importadas() {
        return importadas;
    }

    public String rechazadasJson() {
        return rechazadasJson;
    }

    public String pendientesJson() {
        return pendientesJson;
    }

    public String discrepanciasJson() {
        return discrepanciasJson;
    }

    public String desajustesJson() {
        return desajustesJson;
    }

    public Instant iniciada() {
        return iniciada;
    }

    public Instant terminada() {
        return terminada;
    }
}
