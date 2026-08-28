package com.lautaro.multimetrico.domain;

import com.lautaro.multimetrico.enumeration.EscalaDeNumero;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Cómo leer el número de una celda y llevarlo a las unidades del pliego.
 *
 * <p>{@code 80,5%}, {@code 0.805} y {@code 80.5} son el mismo desempeño escrito
 * de tres maneras, y el pliego evalúa contra una sola: la banda de FCR va de
 * {@code 79.5} a {@code 80.1}, o sea <b>puntos porcentuales</b>. Elegir mal
 * entre esas tres formas no rompe nada de manera visible — produce un número
 * cien veces más grande o más chico, que cae en otra banda y sale por la API
 * como si fuera cierto. Por eso la unidad es parte del perfil y no una
 * suposición del código.
 *
 * <p>Son dos ejes independientes: el {@link SeparadorDecimal} es un problema del
 * texto —una celda numérica ya llega en binario y ahí no hay coma ni punto que
 * interpretar— y la {@link EscalaDeNumero} dice en qué unidad viene el número
 * cuando la celda no lo aclara.
 *
 * <p>Va embebido en {@link PerfilDeImportacion} y se aplana en dos columnas en
 * vez de inventar nombres compuestos: son dos enums y así se ven en un
 * {@code SELECT}.
 *
 * <p>Interpretar una celda con este formato <b>no es un método de acá</b>: es
 * {@code NumeroUtil.interpretar}. Un objeto embebido de persistencia no tiene por
 * qué conocer el {@code ValorCrudo} que sale del lector de planillas.
 */
@Embeddable
public class FormatoDeNumero {

    @Enumerated(EnumType.STRING)
    @Column(name = "separador_decimal", nullable = false, length = 20)
    private SeparadorDecimal separadorDecimal;

    @Enumerated(EnumType.STRING)
    @Column(name = "escala_de_numero", nullable = false, length = 20)
    private EscalaDeNumero escala;

    protected FormatoDeNumero() {
    }

    public FormatoDeNumero(SeparadorDecimal separadorDecimal, EscalaDeNumero escala) {
        if (separadorDecimal == null) {
            throw new IllegalArgumentException("El formato de número necesita un separador decimal");
        }
        if (escala == null) {
            throw new IllegalArgumentException("El formato de número necesita una escala");
        }
        this.separadorDecimal = separadorDecimal;
        this.escala = escala;
    }

    /** Coma decimal y sin escalar: la planilla en español típica. */
    public static FormatoDeNumero estandar() {
        return new FormatoDeNumero(SeparadorDecimal.COMA, EscalaDeNumero.TAL_CUAL);
    }

    public SeparadorDecimal separadorDecimal() {
        return separadorDecimal;
    }

    public EscalaDeNumero escala() {
        return escala;
    }
}
