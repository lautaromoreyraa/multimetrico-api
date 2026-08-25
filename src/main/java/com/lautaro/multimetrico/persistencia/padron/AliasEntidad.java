package com.lautaro.multimetrico.persistencia.padron;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un nombre raro que alguien ya confirmó a qué legajo corresponde.
 *
 * <p>No cuelga del padrón y no tiene período a propósito: es conocimiento que se
 * acumula entre meses. Ver {@code RegistroDeAlias}.
 */
@Entity
@Table(name = "alias_de_nombre")
public class AliasEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * El nombre ya normalizado por {@code NombreNormalizado}: mayúsculas, sin
     * acentos, solo A-Z 0-9 y espacio. Por eso acá la unique de la tabla alcanza
     * sin una columna de clave aparte.
     */
    @Column(name = "nombre", nullable = false, length = 200, unique = true)
    private String nombre;

    @Column(name = "legajo", nullable = false, length = 40)
    private String legajo;

    protected AliasEntidad() {
    }

    AliasEntidad(String nombre, String legajo) {
        this.nombre = nombre;
        this.legajo = legajo;
    }

    String nombre() {
        return nombre;
    }

    String legajo() {
        return legajo;
    }

    void apuntarA(String otroLegajo) {
        this.legajo = otroLegajo;
    }
}
