package com.lautaro.multimetrico.persistencia.padron;

import com.lautaro.multimetrico.padron.domain.Padron;
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

@Entity
@Table(name = "padron")
public class PadronEntidad {

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
     * —{@code equipoDe} ordena por nombre—, guardar una lista y leerla barajada
     * es la clase de diferencia que aparece recién cuando alguien compara dos
     * listados y no entiende por qué no coinciden.
     */
    @OneToMany(mappedBy = "padron", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<AsesorEntidad> asesores = new ArrayList<>();

    protected PadronEntidad() {
    }

    static PadronEntidad de(Padron padron) {
        PadronEntidad entidad = new PadronEntidad();
        entidad.periodo = padron.periodo();

        int orden = 0;
        for (var asesor : padron.asesores()) {
            entidad.asesores.add(AsesorEntidad.de(entidad, asesor, orden++));
        }
        return entidad;
    }

    YearMonth periodo() {
        return periodo;
    }

    Padron aDominio() {
        return new Padron(periodo, asesores.stream().map(AsesorEntidad::aDominio).toList());
    }
}
