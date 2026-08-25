package com.lautaro.multimetrico.persistencia.padron;

import com.lautaro.multimetrico.padron.domain.Asesor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "asesor")
public class AsesorEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "padron_id", nullable = false)
    private PadronEntidad padron;

    @Column(name = "legajo", nullable = false, length = 40)
    private String legajo;

    @Column(name = "nombre", nullable = false, length = 160)
    private String nombre;

    /**
     * El líder como texto, no como referencia a otra fila.
     *
     * <p>El líder llega en la misma planilla que el asesor y se compara
     * normalizado ({@code NombreNormalizado}); no es una entidad del sistema, no
     * tiene legajo propio en el padrón y cambia de escritura archivo a archivo.
     * Una tabla de líderes obligaría a inventar identidad para algo que el
     * dominio trata como un nombre.
     */
    @Column(name = "lider", nullable = false, length = 160)
    private String lider;

    @Column(name = "orden", nullable = false)
    private int orden;

    protected AsesorEntidad() {
    }

    static AsesorEntidad de(PadronEntidad padron, Asesor asesor, int orden) {
        AsesorEntidad entidad = new AsesorEntidad();
        entidad.padron = padron;
        entidad.legajo = asesor.legajo();
        entidad.nombre = asesor.nombre();
        entidad.lider = asesor.lider();
        entidad.orden = orden;
        return entidad;
    }

    Asesor aDominio() {
        return new Asesor(legajo, nombre, lider);
    }
}
