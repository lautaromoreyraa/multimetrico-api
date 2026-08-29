package com.lautaro.multimetrico.domain;

import com.lautaro.multimetrico.util.NombreNormalizado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Una persona del padrón.
 *
 * <p>El {@code legajo} es la identidad de negocio. Es el único dato estable que
 * atraviesa todos los sistemas —el nombre se escribe distinto en cada archivo y
 * cambia cuando alguien se casa—, así que todo lo demás cuelga de él. Los
 * archivos de métricas no lo traen, y por eso hace falta resolverlos por nombre;
 * el punto es que apenas se resuelve una fila, deja de depender del texto.
 *
 * <p>El {@code lider} es por lo que hoy cada supervisor filtra a mano, todas las
 * semanas, en cada archivo de cada métrica. Va como texto y no como referencia a
 * otra fila: llega en la misma planilla que el asesor, se compara normalizado, no
 * tiene legajo propio en el padrón y cambia de escritura archivo a archivo. Una
 * tabla de líderes obligaría a inventar identidad para algo que el dominio trata
 * como un nombre.
 */
@Entity
@Table(name = "asesor")
public class Asesor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "padron_id", nullable = false)
    private Padron padron;

    @Column(name = "legajo", nullable = false, length = 40)
    private String legajo;

    @Column(name = "nombre", nullable = false, length = 160)
    private String nombre;

    @Column(name = "lider", nullable = false, length = 160)
    private String lider;

    @Column(name = "orden", nullable = false)
    private int orden;

    protected Asesor() {
    }

    public Asesor(String legajo, String nombre, String lider) {
        if (legajo == null || legajo.isBlank()) {
            throw new IllegalArgumentException("El asesor necesita un legajo");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El asesor %s necesita un nombre".formatted(legajo));
        }
        if (lider == null || lider.isBlank()) {
            throw new IllegalArgumentException("El asesor %s necesita un líder".formatted(legajo));
        }
        this.legajo = legajo.trim();
        this.nombre = nombre;
        this.lider = lider;
    }

    /**
     * Corrige el nombre o el equipo.
     *
     * <p><b>El legajo no se toca</b>, y es la parte importante. Es la identidad
     * con la que se guardaron las mediciones y los incentivos: cambiarlo dejaría
     * todos esos hechos apuntando a alguien que ya no existe, y la persona
     * aparecería en la tabla sin ninguno de sus números. Si un legajo está mal,
     * lo correcto es dar de baja y volver a dar de alta.
     *
     * <p>El nombre sí se corrige seguido —llega escrito por otro sistema y a
     * veces truncado— y el equipo cambia cuando alguien se muda de célula, que es
     * el caso que más se usa.
     */
    public void corregir(String nombre, String lider) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El asesor %s necesita un nombre".formatted(legajo));
        }
        if (lider == null || lider.isBlank()) {
            throw new IllegalArgumentException("El asesor %s necesita un líder".formatted(legajo));
        }
        this.nombre = nombre.trim();
        this.lider = lider.trim();
    }

    void colgarDe(Padron padron, int orden) {
        this.padron = padron;
        this.orden = orden;
    }

    public Long id() {
        return id;
    }

    public String legajo() {
        return legajo;
    }

    public String nombre() {
        return nombre;
    }

    public String lider() {
        return lider;
    }

    public int orden() {
        return orden;
    }

    public NombreNormalizado nombreNormalizado() {
        return NombreNormalizado.de(nombre);
    }

    public NombreNormalizado liderNormalizado() {
        return NombreNormalizado.de(lider);
    }
}
