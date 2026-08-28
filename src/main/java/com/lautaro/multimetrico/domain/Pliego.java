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

/**
 * Los objetivos de un mes: qué métricas se miden, con qué bandas y con qué peso.
 *
 * <p>Es la entidad central del sistema, y la razón está en el proceso que
 * reemplaza. En la planilla hay <em>una</em> hoja de pliego: la del mes vigente.
 * Cada mes se pisa con la nueva, así que los pliegos anteriores dejan de existir
 * y el cálculo de un mes viejo no se puede reproducir — aplicarle las bandas de
 * hoy a los datos de marzo da otro número.
 *
 * <p>Acá el pliego está atado a su período y no se pisa nunca. Recalcular marzo
 * usa el pliego de marzo. De ahí sale, además, la única lectura que la planilla
 * no puede dar: si un asesor mantuvo su NPS y cobró menos, se puede separar
 * cuánto se movió él de cuánto se movió la vara.
 *
 * <p>La campaña está desde el principio aunque hoy se use una sola. Los
 * multimétricos de la operación ya vienen separados por segmento, así que es
 * cuestión de tiempo que dos campañas convivan con objetivos distintos en el
 * mismo mes; cuesta una columna ahora y un rediseño después.
 *
 * <p><b>Evaluar mediciones contra este pliego no es un método de acá</b>: vive en
 * {@code PliegoServiceImpl}. La entidad sabe qué escalas tiene y cuál es su
 * alcance máximo; producir una {@code Evaluacion} es armar un resultado, y los
 * resultados los arma la capa de servicio.
 */
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

    /**
     * La campaña normalizada, que es por donde se busca.
     *
     * <p>Se guarda además del texto original porque la búsqueda no puede
     * depender de la collation: MySQL compara sin distinguir mayúsculas y H2 sí
     * las distingue, así que "CONVERGENTES" y "Convergentes" serían la misma
     * campaña en producción y dos distintas en los tests.
     */
    @Column(name = "campania_clave", nullable = false, length = 120)
    private String campaniaClave;

    /**
     * Las escalas son un {@link Set} por la misma razón que las bandas de
     * {@link Escala}, y por una segunda que muerde más fuerte.
     *
     * <p>La primera: Hibernate se niega a traer dos <em>bags</em> —listas sin
     * columna de índice— en la misma consulta, y acá se traen las dos de una con
     * un solo {@code @EntityGraph}.
     *
     * <p>La segunda, que es la que hay que tener presente: el {@code join} contra
     * las bandas <b>multiplica las filas de escala</b>, una por cada banda que
     * cuelga. Con una lista, esa duplicación entra tal cual en la colección y el
     * alcance máximo —que es la suma de los pesos— se infla: en el pliego de los
     * tests, 10,5 se convertía en 28. No falla nada, no hay excepción, y el número
     * sale por la API como si fuera cierto. Un {@link Set} lo deduplica solo.
     */
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

    /** El alcance máximo posible: la suma de los pesos de todas las escalas. */
    public BigDecimal alcanceMaximo() {
        return escalas.stream()
                .map(Escala::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
