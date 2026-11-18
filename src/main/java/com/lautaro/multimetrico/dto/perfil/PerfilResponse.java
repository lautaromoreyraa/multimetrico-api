package com.lautaro.multimetrico.dto.perfil;

import com.lautaro.multimetrico.enumeration.EscalaDeNumero;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;

public record PerfilResponse(
        String nombre,
        Metrica metrica,
        String hoja,
        int filaEncabezados,
        String columnaAsesor,
        String columnaValor,
        String columnaLider,
        FormatoDeNumeroResponse formatoNumero) {

    public record FormatoDeNumeroResponse(SeparadorDecimal separadorDecimal, EscalaDeNumero escala) {
    }
}
