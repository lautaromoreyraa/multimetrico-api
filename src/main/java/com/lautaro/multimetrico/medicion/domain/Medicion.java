package com.lautaro.multimetrico.medicion.domain;

import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * El valor que una métrica tenía para un asesor a una fecha de corte.
 *
 * <p><b>Es un hecho histórico, no un casillero que se pisa.</b> La planilla que
 * este proyecto reemplaza tiene una celda por asesor y por métrica: cuando llega
 * el archivo del lunes, se pega encima del que estaba y el número anterior deja
 * de existir. Entonces, cuando un asesor pregunta por qué su FCR bajó dos puntos
 * entre el martes y el jueves, no hay con qué contestarle — el número viejo no
 * está en ningún lado.
 *
 * <p>Acá cada carga semanal es una medición <em>nueva</em>. Dentro del mes queda
 * la serie entera, y de ahí salen las dos cosas que la planilla no puede dar: la
 * evolución dentro del mes, y la explicación de por qué un número cambió.
 *
 * <p>La {@code fechaDeCorte} es el dato que hace posible todo lo demás y es el
 * que la planilla pierde. <b>Cada métrica tiene su propio corte</b>: es normal
 * tener el TMO al viernes pasado y el FCR al lunes anterior, porque los archivos
 * de origen se publican por separado. El multimétrico las junta, y si no dice a
 * qué fecha corresponde cada una, está mostrando una foto que nunca existió.
 *
 * <p>Ojo con confundir {@code periodo} y {@code fechaDeCorte}. El período es el
 * mes que se está midiendo —el que elige el pliego con el que se evalúa—; el
 * corte es el día hasta el que el archivo acumuló. No tienen por qué caer en el
 * mismo mes: el cierre de agosto suele llegar en los primeros días de septiembre.
 *
 * <p>El {@code origenImportacion} dice de qué carga salió este número. Hoy puede
 * venir en {@code null} porque las mediciones se cargan a mano por la API;
 * cuando entre el módulo de importación va a traer el id de la importación, y
 * con eso se puede ir del número mostrado al archivo y a la fila que lo produjo.
 */
public record Medicion(
        String legajo,
        Metrica metrica,
        YearMonth periodo,
        BigDecimal valor,
        LocalDate fechaDeCorte,
        String origenImportacion) {

    public Medicion {
        if (legajo == null || legajo.isBlank()) {
            throw new IllegalArgumentException("La medición necesita un legajo");
        }
        if (metrica == null) {
            throw new IllegalArgumentException("La medición de %s necesita una métrica".formatted(legajo));
        }
        if (periodo == null) {
            throw new IllegalArgumentException(
                    "La medición de %s en %s necesita un período".formatted(legajo, metrica));
        }
        // Un valor nulo no es un cero: es una métrica que no llegó, y eso se
        // representa no cargando la medición, no cargándola vacía.
        if (valor == null) {
            throw new IllegalArgumentException(
                    "La medición de %s en %s necesita un valor. Una métrica sin dato no se carga en cero: "
                            .formatted(legajo, metrica)
                            + "se deja faltante.");
        }
        if (fechaDeCorte == null) {
            throw new IllegalArgumentException(
                    ("La medición de %s en %s necesita una fecha de corte. Sin ella no se sabe cuál de dos "
                            + "cargas es la vigente ni a qué día corresponde el número.").formatted(legajo, metrica));
        }
        legajo = legajo.trim();
    }

    /**
     * Si esta medición desplaza a {@code otra}, que es la que venía valiendo.
     *
     * <p>Gana la del corte más reciente. <b>Empatado el corte, gana ésta</b>, y
     * el criterio no es arbitrario: dos mediciones con la misma fecha de corte
     * son dos cargas del mismo archivo, y la segunda es una corrección de la
     * primera. Quedarse con la más vieja dejaría sin efecto justamente la
     * corrección que alguien se tomó el trabajo de subir.
     *
     * <p>Eso significa algo solo si quien recorre la historia lo hace <b>en
     * orden de carga</b>, tratando cada medición como candidata a desplazar a la
     * que quedó atrás. El repositorio devuelve la historia en ese orden, y es lo
     * que hace {@link MedicionesVigentes}.
     */
    public boolean desplazaA(Medicion otra) {
        return !fechaDeCorte.isBefore(otra.fechaDeCorte());
    }

    public boolean esDe(String otroLegajo) {
        return legajo.equalsIgnoreCase(otroLegajo == null ? "" : otroLegajo.trim());
    }
}
