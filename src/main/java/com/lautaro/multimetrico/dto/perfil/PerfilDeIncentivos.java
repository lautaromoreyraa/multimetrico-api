package com.lautaro.multimetrico.dto.perfil;

import com.lautaro.multimetrico.domain.FormatoDeNumero;
import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.enumeration.EscalaDeNumero;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;
import jakarta.validation.constraints.NotBlank;

/**
 * Cómo leer el archivo de incentivos.
 *
 * <h2>Por qué no se guarda como los perfiles de métricas</h2>
 *
 * <p>Los perfiles de importación se guardan porque el archivo semanal de cada
 * métrica llega todas las semanas con el mismo formato: definirlo una vez y
 * reusarlo es el punto. El de incentivos llega una vez por mes, así que
 * guardarlo ahorra poco y agrega una pantalla de administración más.
 *
 * <p>Viaja entonces como parámetros de la carga. Si algún día molesta repetirlo,
 * se guarda — pero mientras se use una vez al mes, el formulario alcanza.
 *
 * <h2>Las columnas se eligen por su encabezado</h2>
 *
 * <p>Igual que en los perfiles de métricas, y por la misma razón: nadie tiene
 * acceso a los archivos que publica la empresa y van a cambiar. Una columna que
 * se corre de lugar sigue llamándose igual; una que se identifica por la letra de
 * Excel, no.
 */
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

    /**
     * El mismo perfil, con la forma que espera {@code LectorDePlanilla}.
     *
     * <p>El lector ya sabe abrir un {@code .xlsx}, encontrar la hoja, ubicar los
     * encabezados y traer las celdas con su número. Reusarlo es lo correcto:
     * escribir un segundo lector para un archivo con dos columnas sería tener dos
     * versiones de las mismas decisiones difíciles esperando a despegarse.
     *
     * <p>La métrica es un relleno que el lector no mira. Se pone {@code TMO} y no
     * {@code null} porque el constructor del perfil la exige, con razón: para una
     * importación de métricas, no saber cuál trae el archivo es un error. Acá no
     * significa nada.
     *
     * <p>La escala va {@code TAL_CUAL}: un importe es un importe. La conversión de
     * fracción a porcentaje existe para las métricas que llegan como 0,80 en vez
     * de 80, y aplicarla a plata multiplicaría los sueldos por cien.
     */
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
