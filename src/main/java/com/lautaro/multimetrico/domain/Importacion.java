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

/**
 * Una carga de archivo, con su informe adentro.
 *
 * <p>La clave primaria es el UUID que ya genera el servicio y devuelve la API,
 * no un autoincremental: ese id sale por la respuesta del {@code POST} apenas se
 * recibe el archivo, mucho antes de que la importación termine, y es por donde
 * se la consulta después. Un id interno distinto del público sería un segundo
 * identificador para la misma cosa.
 *
 * <p>Una importación se escribe varias veces sobre la misma fila: primero
 * {@link EstadoDeImportacion#EN_CURSO} apenas llega el archivo, después
 * {@code TERMINADA} o {@code RECHAZADA} cuando el procesamiento la resuelve, y
 * otra vez cada vez que alguien resuelve un pendiente a mano. Por eso tiene
 * mutadores y el resto de las entidades no.
 *
 * <p>Las cuatro listas del informe van serializadas en columnas de texto; el
 * porqué está en {@code JsonUtil}, y quien las arma y las lee es
 * {@code ImportacionMapper}. Acá viajan como {@link String} a propósito: la
 * entidad no tiene por qué conocer los DTOs del informe, y la serialización de lo
 * que se guarda no puede cambiar porque alguien ajustó cómo se ve la API.
 *
 * <p>{@code iniciada} la pone la auditoría de Spring Data ({@code @CreatedDate}),
 * no el código: es el instante del alta y no hay ningún caso en que convenga
 * poder pasarlo a mano. {@code terminada} sí es del negocio —es cuándo dejó de
 * procesar— y la escribe el servicio.
 */
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

    /**
     * Los dos contadores sí son columnas: son lo único del informe que se cuenta
     * o se compara entre importaciones, y son los que sostienen la invariante
     * "importadas + rechazadas + pendientes == filas leídas" que revalida
     * {@code Informe} al reconstruirse.
     */
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

    /** Una importación recién recibida: sin informe todavía. */
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

    /**
     * Deja anotado el informe sin tocar el estado.
     *
     * <p>Es lo que hace falta cuando alguien resuelve un pendiente a mano: la
     * importación ya estaba {@code TERMINADA} y sigue estándolo, pero una fila
     * pasó de pendiente a importada.
     */
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

    /**
     * El archivo no coincidía con el perfil: no entró <b>nada</b>.
     *
     * <p>Los contadores quedan en cero y el informe vacío. Es distinto de
     * terminar con rechazos, y la pantalla lo muestra distinto.
     */
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
