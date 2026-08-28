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

/**
 * Quién estaba en la operación en un período, y bajo qué líder.
 *
 * <p>Va por período como el pliego, y por la misma razón: la gente entra y sale,
 * y el multimétrico de marzo tiene que poder rearmarse con la gente que había en
 * marzo. Si el padrón fuera uno solo y siempre el actual, un asesor que renunció
 * desaparecería de sus propios meses.
 *
 * <p><b>Pero a diferencia del pliego, el padrón sí se puede reemplazar.</b> No es
 * un descuido de consistencia: son cosas distintas. El pliego es el compromiso
 * del mes y pisarlo cambia resultados ya calculados; el padrón es una lista de
 * personas que cambia dentro del mismo mes cada vez que alguien ingresa. Volver
 * a cargarlo es la operación normal, no una corrección excepcional.
 *
 * <p>Guarda <b>todos</b> los equipos, no solo uno. Es la forma del archivo de
 * origen —la nómina viene con la operación entera y una columna de líder— y es lo
 * que permite que el sistema haga el filtrado que hoy hace cada supervisor a mano.
 */
@Entity
@Table(name = "padron")
public class Padron {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "periodo", nullable = false, length = 7, unique = true)
    private YearMonth periodo;

    /**
     * Los asesores en el orden en que llegaron.
     *
     * <p>La columna {@code orden} existe para eso. Sin ella el orden lo decidiría
     * el motor, y aunque las vistas que importan reordenan por su cuenta
     * —{@link #equipoDe} ordena por nombre—, guardar una lista y leerla barajada
     * es la clase de diferencia que aparece recién cuando alguien compara dos
     * listados y no entiende por qué no coinciden.
     */
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

    /**
     * El equipo de un líder.
     *
     * <p>Esto es el paso que hoy se hace a mano: abrir el archivo con todos los
     * equipos y filtrar por el propio nombre, en cada métrica, todas las semanas.
     */
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
