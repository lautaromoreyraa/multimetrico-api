package com.lautaro.multimetrico.importacion.web;

import com.lautaro.multimetrico.importacion.domain.FormatoDeNumero;
import com.lautaro.multimetrico.importacion.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Un perfil tal como llega por HTTP.
 *
 * <p>La fila de encabezados viene con la numeración que se ve en Excel —la
 * primera fila es la 1—, no con el índice interno de POI. Quien arma un perfil
 * está mirando la planilla abierta, y pedirle que reste uno es pedirle que se
 * equivoque.
 */
public record PerfilRequest(
        @NotBlank(message = "el perfil necesita un nombre") String nombre,
        @NotNull(message = "hay que decir qué métrica trae el archivo") Metrica metrica,
        String hoja,
        @Positive(message = "la fila de encabezados se cuenta desde 1") int filaEncabezados,
        @NotBlank(message = "hace falta el encabezado de la columna del asesor") String columnaAsesor,
        @NotBlank(message = "hace falta el encabezado de la columna del valor") String columnaValor,
        String columnaLider,
        FormatoDeNumero.SeparadorDecimal separadorDecimal,
        FormatoDeNumero.Escala escala) {

    public PerfilDeImportacion aDominio() {
        return new PerfilDeImportacion(nombre, metrica, hoja, filaEncabezados,
                columnaAsesor, columnaValor, columnaLider,
                new FormatoDeNumero(
                        separadorDecimal == null ? FormatoDeNumero.SeparadorDecimal.COMA : separadorDecimal,
                        escala == null ? FormatoDeNumero.Escala.TAL_CUAL : escala));
    }
}
