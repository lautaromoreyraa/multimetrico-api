package com.lautaro.multimetrico.multimetrico.domain;

import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * El multimétrico de un equipo: una fila por asesor, más lo que hace falta para
 * leerlas juntas.
 *
 * <p>El {@code lider} puede venir en {@code null}, y ahí son todos los asesores
 * del período. Es la vista que hoy no existe: cada supervisor arma la suya
 * filtrando el archivo por su propio nombre, así que nadie ve la operación
 * entera con los mismos criterios aplicados.
 *
 * <p>El {@code alcanceMaximo} viene del pliego y no de las filas. Sin él, un
 * total de 9,5 no significa nada; con él se sabe si es 9,5 sobre 12 o sobre 20.
 */
public record Multimetrico(
        YearMonth periodo,
        String campania,
        String lider,
        BigDecimal alcanceMaximo,
        List<FilaDeMultimetrico> filas) {

    public Multimetrico {
        filas = List.copyOf(filas);
    }

    public int asesores() {
        return filas.size();
    }

    public long completos() {
        return filas.stream().filter(FilaDeMultimetrico::completa).count();
    }

    public long conFaltantes() {
        return filas.stream().filter(fila -> !fila.faltantes().isEmpty()).count();
    }

    /**
     * Las fechas de corte que aparecen en el equipo, por métrica.
     *
     * <p>Lo normal es una sola fecha por métrica: el archivo semanal trae a todo
     * el mundo con el mismo corte. <b>Que haya dos es una señal</b> —quedó gente
     * afuera del último archivo y sigue mostrando el número de la semana
     * pasada—, y es exactamente la clase de cosa que hoy no se ve hasta que
     * alguien reclama.
     */
    public Map<Metrica, Set<LocalDate>> cortesDelEquipo() {
        Map<Metrica, Set<LocalDate>> cortes = new EnumMap<>(Metrica.class);
        for (FilaDeMultimetrico fila : filas) {
            fila.cortesPorMetrica().forEach((metrica, fecha) -> cortes
                    .computeIfAbsent(metrica, clave -> new LinkedHashSet<>())
                    .add(fecha));
        }
        cortes.replaceAll((metrica, fechas) -> fechas.stream()
                .sorted(Comparator.naturalOrder())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
        return cortes;
    }

    /**
     * El corte más viejo de todo el equipo: la fecha hasta la que se puede decir
     * que este multimétrico está completo.
     */
    public Optional<LocalDate> actualizadoDesde() {
        return filas.stream()
                .map(FilaDeMultimetrico::corteMasViejo)
                .flatMap(Optional::stream)
                .min(Comparator.naturalOrder());
    }
}
