package com.lautaro.multimetrico.dto.perfil;

import com.lautaro.multimetrico.enumeration.EscalaDeNumero;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;

/**
 * Un perfil tal como sale por HTTP.
 *
 * <p>Sale anidado —{@code formatoNumero} adentro— aunque entra plano, y no es un
 * descuido: el que lee un perfil quiere ver que la interpretación del número es
 * una cosa con dos partes, y el que lo escribe está completando dos campos de un
 * formulario. Son dos lecturas distintas del mismo dato.
 *
 * <p>Lo que no lleva es el {@code id} de la fila: la identidad de un perfil es su
 * nombre, que es por donde se lo pide al importar.
 */
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
