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

/**
 * La tabla de bandas de una métrica dentro de un pliego, con su peso.
 *
 * <p>El peso es el techo: lo máximo que esa métrica puede aportar al alcance
 * total. En la planilla viaja metido en el título de la tabla —"NPS (3,5%)"—,
 * acá es una columna, porque es un dato del negocio y no un rótulo.
 *
 * <p>Esta entidad no valida que las bandas sean coherentes. De eso se encarga
 * {@code ValidadorDePliego}, y la razón es práctica: quien carga un pliego
 * quiere ver <em>todos</em> los problemas de una vez, no el primero que
 * aparezca. Un constructor que revienta sirve para proteger un invariante, no
 * para redactar un informe.
 */
@Entity
@Table(name = "escala")
public class Escala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pliego_id", nullable = false)
    private Pliego pliego;

    /**
     * El nombre de la métrica, no su posición en el enum.
     *
     * <p>{@code EnumType.ORDINAL} guardaría un 0 que cambia de significado si
     * alguien agrega una métrica en el medio de la lista, y el pliego de un mes
     * viejo pasaría a evaluar otra cosa. Un pliego que no se puede reproducir es
     * el defecto que este proyecto vino a corregir.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "metrica", nullable = false, length = 40)
    private Metrica metrica;

    @Column(name = "peso", nullable = false, precision = 18, scale = 6)
    private BigDecimal peso;

    @Column(name = "orden", nullable = false)
    private int orden;

    /**
     * Las bandas son un {@link Set} y no una {@link List}, y no es cosmético.
     *
     * <p>Hibernate llama <em>bag</em> a una {@code List} sin columna de índice, y
     * se niega a traer dos bags en la misma consulta —{@code MultipleBagFetchException}—
     * <b>incluso cuando uno cuelga del otro</b>. El pliego trae sus escalas y cada
     * escala sus bandas con un solo {@code @EntityGraph}, así que una de las dos
     * colecciones tiene que dejar de ser un bag.
     *
     * <p>Se eligió ésta porque es la que no pierde nada al convertirse: una banda
     * no se repite dentro de una escala —dos bandas idénticas serían un pliego
     * roto que el validador rechaza— y el orden lo sigue dando {@code @OrderBy},
     * que Hibernate respeta materializando un {@code LinkedHashSet}.
     *
     * <p>La alternativa era {@code @OrderColumn}, que le daría a Hibernate la
     * propiedad de la columna {@code orden}. Se descartó: hoy ese número lo
     * escribe {@link #Escala} al colgar cada banda, y cederlo significaría que el
     * orden de carga pasa a depender de cuándo Hibernate decide reindexar.
     */
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

    /**
     * Las bandas ordenadas de menor a mayor. La abierta hacia abajo va primero
     * y la abierta hacia arriba, última.
     */
    public List<Banda> ordenadas() {
        return bandas.stream()
                .sorted(Comparator.comparing(Banda::desde, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * En qué banda cae un valor.
     *
     * <p>Devuelve {@link Optional} y no una banda por defecto a propósito: si un
     * valor no cae en ninguna, eso es un pliego mal armado y hay que verlo, no
     * taparlo con un cero. Sobre un pliego validado nunca está vacío.
     */
    public Optional<Banda> bandaPara(BigDecimal valor) {
        return bandas.stream().filter(banda -> banda.contiene(valor)).findFirst();
    }

    /** El mejor resultado que ofrece la tabla; tiene que coincidir con el peso. */
    public BigDecimal mejorResultado() {
        return bandas.stream()
                .map(Banda::resultado)
                .max(BigDecimal::compareTo)
                .orElseThrow();
    }
}
