package com.lautaro.multimetrico.dto.perfil;

import com.lautaro.multimetrico.enumeration.EscalaDeNumero;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PerfilRequest(
        @NotBlank(message = "el perfil necesita un nombre") String nombre,
        @NotNull(message = "hay que decir qué métrica trae el archivo") Metrica metrica,
        String hoja,
        @Positive(message = "la fila de encabezados se cuenta desde 1") int filaEncabezados,
        @NotBlank(message = "hace falta el encabezado de la columna del asesor") String columnaAsesor,
        @NotBlank(message = "hace falta el encabezado de la columna del valor") String columnaValor,
        String columnaLider,
        SeparadorDecimal separadorDecimal,
        EscalaDeNumero escala) {
}
