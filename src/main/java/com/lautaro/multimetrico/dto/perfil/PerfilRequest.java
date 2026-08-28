package com.lautaro.multimetrico.dto.perfil;

import com.lautaro.multimetrico.enumeration.EscalaDeNumero;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;
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
 *
 * <p>{@code separadorDecimal} y {@code escala} llegan planos y no anidados en un
 * objeto: es lo que ve quien completa el formulario, dos preguntas sueltas. El
 * mapper los junta en el {@code FormatoDeNumero} que guarda la base.
 */
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
