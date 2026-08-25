package com.lautaro.multimetrico.persistencia.importacion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lautaro.multimetrico.importacion.domain.Desajuste;
import com.lautaro.multimetrico.importacion.domain.DiscrepanciaDeLider;
import com.lautaro.multimetrico.importacion.domain.FilaPendiente;
import com.lautaro.multimetrico.importacion.domain.FilaRechazada;
import com.lautaro.multimetrico.importacion.domain.Importacion;
import com.lautaro.multimetrico.importacion.domain.Informe;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Una importación como fila, con su informe adentro.
 *
 * <p>La clave primaria es el UUID que ya genera el servicio y devuelve la API,
 * no un autoincremental: ese id sale por la respuesta del {@code POST} apenas se
 * recibe el archivo, mucho antes de que la importación termine, y es por donde
 * se la consulta después. Un id interno distinto del público sería un segundo
 * identificador para la misma cosa.
 *
 * <p>Las cuatro listas del informe van serializadas en columnas de texto. El
 * porqué está en {@link ListasComoJson}.
 */
@Entity
@Table(name = "importacion")
public class ImportacionEntidad {

    private static final TypeReference<List<FilaRechazada>> RECHAZADAS = new TypeReference<>() {
    };
    private static final TypeReference<List<FilaPendiente>> PENDIENTES = new TypeReference<>() {
    };
    private static final TypeReference<List<DiscrepanciaDeLider>> DISCREPANCIAS = new TypeReference<>() {
    };
    private static final TypeReference<List<Desajuste>> DESAJUSTES = new TypeReference<>() {
    };

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
    private Importacion.Estado estado;

    /**
     * Los dos contadores sí son columnas: son lo único del informe que se cuenta
     * o se compara entre importaciones, y son los que sostienen la invariante
     * "importadas + rechazadas + pendientes == filas leídas" que revalida
     * {@link Informe} al reconstruirse.
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

    @Column(name = "iniciada", nullable = false)
    private Instant iniciada;

    @Column(name = "terminada")
    private Instant terminada;

    protected ImportacionEntidad() {
    }

    void copiarDe(Importacion importacion) {
        this.id = importacion.id();
        this.perfil = importacion.perfil();
        this.metrica = importacion.metrica();
        this.periodo = importacion.periodo();
        this.fechaDeCorte = importacion.fechaDeCorte();
        this.estado = importacion.estado();

        Informe informe = importacion.informe();
        this.filasLeidas = informe.filasLeidas();
        this.importadas = informe.importadas();
        this.rechazadasJson = ListasComoJson.escribir(informe.rechazadas());
        this.pendientesJson = ListasComoJson.escribir(informe.pendientes());
        this.discrepanciasJson = ListasComoJson.escribir(informe.discrepanciasDeLider());
        this.desajustesJson = ListasComoJson.escribir(importacion.desajustes());

        this.iniciada = importacion.iniciada();
        this.terminada = importacion.terminada();
    }

    static ImportacionEntidad de(Importacion importacion) {
        ImportacionEntidad entidad = new ImportacionEntidad();
        entidad.copiarDe(importacion);
        return entidad;
    }

    Importacion aDominio() {
        Informe informe = new Informe(
                filasLeidas,
                importadas,
                ListasComoJson.leer(rechazadasJson, RECHAZADAS),
                ListasComoJson.leer(pendientesJson, PENDIENTES),
                ListasComoJson.leer(discrepanciasJson, DISCREPANCIAS));

        return new Importacion(id, perfil, metrica, periodo, fechaDeCorte, estado, informe,
                ListasComoJson.leer(desajustesJson, DESAJUSTES), iniciada, terminada);
    }
}
