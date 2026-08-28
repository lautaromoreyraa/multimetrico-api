package com.lautaro.multimetrico.domain;

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
 * acumula entre meses. Si en marzo alguien confirmó que cierto texto mal
 * codificado es tal legajo, en abril el sistema no vuelve a preguntar. Esa es la
 * diferencia entre una herramienta que aprende y una que hace corregir lo mismo
 * todos los meses.
 */
@Entity
@Table(name = "alias_de_nombre")
public class AliasDeNombre {

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

    protected AliasDeNombre() {
    }

    public AliasDeNombre(String nombre, String legajo) {
        this.nombre = nombre;
        this.legajo = legajo;
    }

    public Long id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String legajo() {
        return legajo;
    }

    /**
     * Confirmar un nombre que ya estaba lo reapunta, no lo duplica.
     *
     * <p>Si alguien se equivocó al confirmar, volver a confirmar corrige. Un
     * insert a secas chocaría contra la unique del nombre y dejaría el error
     * escrito para siempre.
     */
    public void apuntarA(String otroLegajo) {
        this.legajo = otroLegajo;
    }
}
