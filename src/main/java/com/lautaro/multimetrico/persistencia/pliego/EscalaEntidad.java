package com.lautaro.multimetrico.persistencia.pliego;

import com.lautaro.multimetrico.persistencia.Decimales;
import com.lautaro.multimetrico.pliego.domain.Escala;
import com.lautaro.multimetrico.pliego.domain.Metrica;
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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "escala")
public class EscalaEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pliego_id", nullable = false)
    private PliegoEntidad pliego;

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

    @OneToMany(mappedBy = "escala", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<BandaEntidad> bandas = new ArrayList<>();

    protected EscalaEntidad() {
    }

    static EscalaEntidad de(PliegoEntidad pliego, Escala escala, int orden) {
        EscalaEntidad entidad = new EscalaEntidad();
        entidad.pliego = pliego;
        entidad.metrica = escala.metrica();
        entidad.peso = escala.peso();
        entidad.orden = orden;

        int ordenDeBanda = 0;
        for (var banda : escala.bandas()) {
            entidad.bandas.add(BandaEntidad.de(entidad, banda, ordenDeBanda++));
        }
        return entidad;
    }

    Escala aDominio() {
        return new Escala(metrica, Decimales.comoEntro(peso), bandas.stream().map(BandaEntidad::aDominio).toList());
    }
}
