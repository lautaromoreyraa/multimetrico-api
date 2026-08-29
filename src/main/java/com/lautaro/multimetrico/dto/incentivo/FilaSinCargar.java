package com.lautaro.multimetrico.dto.incentivo;

/**
 * Una fila del archivo de incentivos que no se pudo guardar, y por qué.
 *
 * <h2>Por qué no reusa {@code FilaRechazada}</h2>
 *
 * <p>El rechazo de la importación de métricas deriva su explicación de un enum
 * cerrado de motivos, y está bien que así sea: son cuatro situaciones conocidas y
 * tenerlas tipadas evita que cada lugar redacte la suya.
 *
 * <p>Acá hace falta texto libre, y por un caso concreto: cuando un nombre coincide
 * con más de un asesor, el motivo tiene que <b>nombrar a los candidatos</b>. Eso
 * es lo único que le sirve a quien tiene que corregir el archivo, y no entra en un
 * enum. Agregarle una constante a {@code MotivoDeRechazo} habría dado un mensaje
 * genérico —"el nombre es ambiguo"— que obliga a ir a buscar cuáles eran.
 */
public record FilaSinCargar(int numeroDeFila, String textoAsesor, String textoImporte, String motivo) {

    public FilaSinCargar {
        textoAsesor = textoAsesor == null ? "" : textoAsesor;
        textoImporte = textoImporte == null ? "" : textoImporte;
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Un rechazo sin motivo no explica nada");
        }
    }
}
