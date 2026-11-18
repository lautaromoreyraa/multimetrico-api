package com.lautaro.multimetrico.dto.perfil;

import com.lautaro.multimetrico.domain.FormatoDeNumero;
import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.enumeration.EscalaDeNumero;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;
import jakarta.validation.constraints.NotBlank;

public record PerfilDeIncentivos(
        String hoja,
        Integer filaEncabezados,
        @NotBlank(message = "hay que decir qué columna tiene el nombre del asesor") String columnaAsesor,
        @NotBlank(message = "hay que decir qué columna tiene el importe") String columnaImporte,
        SeparadorDecimal separadorDecimal) {

    public PerfilDeIncentivos {
        filaEncabezados = filaEncabezados == null ? 1 : filaEncabezados;
        separadorDecimal = separadorDecimal == null ? SeparadorDecimal.COMA : separadorDecimal;
    }

    public PerfilDeImportacion aPerfilDeLectura() {
        return new PerfilDeImportacion(
                "incentivos",
                Metrica.TMO,
                hoja,
                filaEncabezados,
                columnaAsesor,
                columnaImporte,
                null,
                new FormatoDeNumero(separadorDecimal, EscalaDeNumero.TAL_CUAL));
    }
}
