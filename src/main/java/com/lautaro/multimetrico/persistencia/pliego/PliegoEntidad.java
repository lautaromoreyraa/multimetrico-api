package com.lautaro.multimetrico.persistencia.pliego;

import com.lautaro.multimetrico.pliego.domain.Pliego;
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
import java.util.List;
import java.util.Locale;

/**
 * El pliego como fila, no como record.
 *
 * <p>Es una clase mutable con constructor sin argumentos porque JPA lo exige, y
 * por eso vive acá y no en {@code pliego.domain}: el dominio sigue siendo
 * {@link Pliego}, un record inmutable que no sabe que existe una base. La
 * traducción de ida y vuelta la hace {@link PliegoEnBase} y no sale de este
 * paquete.
 */
@Entity
@Table(name = "pliego")
public class PliegoEntidad {

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

    @OneToMany(mappedBy = "pliego", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<EscalaEntidad> escalas = new ArrayList<>();

    protected PliegoEntidad() {
    }

    public static String clave(String campania) {
        return campania.trim().toUpperCase(Locale.ROOT);
    }

    public static PliegoEntidad de(Pliego pliego) {
        PliegoEntidad entidad = new PliegoEntidad();
        entidad.periodo = pliego.periodo();
        entidad.campania = pliego.campania();
        entidad.campaniaClave = clave(pliego.campania());

        int orden = 0;
        for (var escala : pliego.escalas()) {
            entidad.escalas.add(EscalaEntidad.de(entidad, escala, orden++));
        }
        return entidad;
    }

    public Pliego aDominio() {
        return new Pliego(periodo, campania, escalas.stream().map(EscalaEntidad::aDominio).toList());
    }
}
