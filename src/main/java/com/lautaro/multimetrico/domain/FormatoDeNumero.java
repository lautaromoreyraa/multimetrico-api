package com.lautaro.multimetrico.domain;

import com.lautaro.multimetrico.enumeration.EscalaDeNumero;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

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
