package com.lautaro.multimetrico.service.multimetrico.impl;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Banda;
import com.lautaro.multimetrico.domain.Escala;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.evaluacion.Alcance;
import com.lautaro.multimetrico.dto.evaluacion.Evaluacion;
import com.lautaro.multimetrico.dto.medicion.MedicionesVigentes;
import com.lautaro.multimetrico.dto.multimetrico.CeldaDeMultimetrico;
import com.lautaro.multimetrico.dto.multimetrico.FilaDeMultimetrico;
import com.lautaro.multimetrico.dto.multimetrico.Multimetrico;
import com.lautaro.multimetrico.dto.multimetrico.ResumenDeEquipo;
import com.lautaro.multimetrico.dto.multimetrico.TramoDeArriba;
import com.lautaro.multimetrico.enumeration.CondicionDeTramo;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.Sentido;
import com.lautaro.multimetrico.exception.RecursoNoEncontradoException;
import com.lautaro.multimetrico.mapper.PadronMapper;
import com.lautaro.multimetrico.service.medicion.MedicionService;
import com.lautaro.multimetrico.service.multimetrico.MultimetricoService;
import com.lautaro.multimetrico.service.padron.PadronService;
import com.lautaro.multimetrico.service.pliego.PliegoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El armado del multimétrico.
 *
 * <p>Acá viven los dos cálculos que antes eran métodos de los records de
 * dominio: armar la fila de un asesor y decidir cuál es su tramo de arriba. Los
 * dos son funciones puras —mismo pliego, mismo asesor y mismas mediciones dan
 * siempre lo mismo— y ninguno toca un repositorio: quien elige el pliego del
 * período correcto es el método público que los llama.
 */
@Service
public class MultimetricoServiceImpl implements MultimetricoService {

    private final PliegoService pliegos;
    private final PadronService padrones;
    private final MedicionService mediciones;
    private final PadronMapper padronMapper;

    public MultimetricoServiceImpl(PliegoService pliegos, PadronService padrones,
            MedicionService mediciones, PadronMapper padronMapper) {
        this.pliegos = pliegos;
        this.padrones = padrones;
        this.mediciones = mediciones;
        this.padronMapper = padronMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Multimetrico delEquipo(YearMonth periodo, String lider, String campania) {
        Pliego pliego = pliegos.buscar(periodo, campania);
        Padron padron = padrones.buscar(periodo);

        List<Asesor> equipo = lider == null || lider.isBlank()
                ? padron.asesores().stream().sorted(Comparator.comparing(Asesor::nombre)).toList()
                : padron.equipoDe(lider);

        if (equipo.isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "El líder '%s' no tiene asesores en el padrón de %s. Los líderes cargados son: %s"
                            .formatted(lider, periodo, String.join(", ", padron.lideres())));
        }

        // Una sola pasada por la historia del mes en vez de una por asesor.
        Map<String, MedicionesVigentes> vigentes = mediciones.vigentesDelPeriodo(periodo);

        List<FilaDeMultimetrico> filas = equipo.stream()
                .map(asesor -> armarFila(pliego, asesor,
                        vigentes.getOrDefault(asesor.legajo().toUpperCase(java.util.Locale.ROOT),
                                MedicionesVigentes.ninguna())))
                .toList();

        return new Multimetrico(periodo, pliego.campania(), lider, pliego.alcanceMaximo(),
                resumir(filas), filas);
    }

    @Override
    @Transactional(readOnly = true)
    public FilaDeMultimetrico deUnAsesor(YearMonth periodo, String legajo, String campania) {
        Pliego pliego = pliegos.buscar(periodo, campania);
        Asesor asesor = padrones.buscar(periodo).porLegajo(legajo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El legajo %s no está en el padrón de %s".formatted(legajo, periodo)));

        return armarFila(pliego, asesor, mediciones.vigentesDe(periodo, legajo));
    }

    /**
     * Evalúa las mediciones vigentes de un asesor contra el pliego del período.
     *
     * <p>El pliego que entra tiene que ser el del período que se está armando, no
     * el vigente hoy. Quien lo elige es el método que llama a éste; acá se confía
     * en que ya se eligió bien, y por eso mismo no se toca ningún repositorio.
     *
     * <p>Es visible en el paquete y no privado para que el test lo pueda llamar
     * directo: es una función pura y probarla a través de un endpoint obligaría a
     * levantar una base para verificar una cuenta que no depende de ninguna.
     */
    FilaDeMultimetrico armarFila(Pliego pliego, Asesor asesor, MedicionesVigentes vigentes) {
        Evaluacion evaluacion = pliegos.evaluar(pliego, vigentes.valores());

        List<CeldaDeMultimetrico> celdas = new ArrayList<>();
        for (Alcance alcance : evaluacion.alcances().values()) {
            TramoDeArriba tramo = pliego.escalaDe(alcance.metrica())
                    .flatMap(escala -> tramoDeArriba(escala, alcance))
                    .orElse(null);

            // El corte está sí o sí: el alcance existe porque hubo una medición
            // vigente, y toda medición tiene fecha de corte.
            LocalDate corte = vigentes.corteDe(alcance.metrica()).orElseThrow();
            celdas.add(new CeldaDeMultimetrico(alcance, corte, tramo));
        }
        celdas.sort(Comparator.comparing(celda -> celda.metrica().ordinal()));

        List<LocalDate> cortes = celdas.stream().map(CeldaDeMultimetrico::actualizadoAl).toList();

        return new FilaDeMultimetrico(
                padronMapper.aResponse(asesor),
                celdas,
                evaluacion.faltantes(),
                evaluacion.fueraDeEscala(),
                evaluacion.alcanceTotal(),
                evaluacion.completa(),
                cortes.stream().min(Comparator.naturalOrder()).orElse(null),
                cortes.stream().max(Comparator.naturalOrder()).orElse(null));
    }

    /**
     * El tramo de arriba de un alcance ya calculado, o vacío si no hay ninguno
     * mejor al alcance.
     *
     * <p>No es "la banda siguiente en la lista", porque hacia dónde se mejora
     * depende del {@link Sentido}: en NPS o FCR se sube y en TMO o bajas se baja.
     * Y tampoco se supone que el resultado crezca de forma pareja con el valor
     * —eso es cierto en los pliegos sanos pero nada lo obliga—, así que primero se
     * filtran las bandas que <b>pagan más que la actual</b> y recién después se
     * busca, entre ésas, la que queda más cerca en la dirección en la que la
     * métrica mejora.
     *
     * <p>Vacío significa una de dos cosas: el asesor está en la mejor banda de la
     * escala, o el pliego tiene las bandas que pagan más del lado equivocado —lo
     * que sería un pliego raro, no un error de este cálculo—. En los dos casos lo
     * honesto es no mostrar nada antes que mostrar un objetivo que no mejora.
     *
     * <p>Visible en el paquete por el mismo motivo que {@link #armarFila}: es el
     * cálculo más sutil del proyecto y se prueba directo, banda por banda.
     */
    static Optional<TramoDeArriba> tramoDeArriba(Escala escala, Alcance alcance) {
        BigDecimal valor = alcance.valor();

        var mejores = escala.bandas().stream()
                .filter(banda -> banda.resultado().compareTo(alcance.resultado()) > 0);

        if (escala.metrica().sentido() == Sentido.MAYOR_ES_MEJOR) {
            return mejores
                    // Una banda abierta hacia abajo nunca está "más arriba": su desde es null.
                    .filter(banda -> banda.desde() != null && banda.desde().compareTo(valor) > 0)
                    .min(Comparator.comparing(Banda::desde))
                    .map(banda -> tramo(banda, alcance, banda.desde(),
                            banda.desde().subtract(valor), CondicionDeTramo.ALCANZAR));
        }

        return mejores
                // hasta <= valor, no <: si el valor cae justo en el borde, todavía
                // está del lado de afuera y esa banda sigue siendo la de arriba.
                .filter(banda -> banda.hasta() != null && banda.hasta().compareTo(valor) <= 0)
                .max(Comparator.comparing(Banda::hasta))
                .map(banda -> tramo(banda, alcance, banda.hasta(),
                        valor.subtract(banda.hasta()), CondicionDeTramo.BAJAR_DE));
    }

    private static TramoDeArriba tramo(Banda banda, Alcance actual, BigDecimal objetivo,
            BigDecimal distancia, CondicionDeTramo condicion) {

        return new TramoDeArriba(
                banda.rango(),
                objetivo,
                distancia,
                condicion,
                banda.resultado(),
                banda.resultado().subtract(actual.resultado()));
    }

    /**
     * Las cuentas del equipo, que van arriba de la tabla.
     *
     * <p>{@code cortesPorMetrica} junta las fechas que aparecen en cada métrica.
     * Lo normal es una sola —el archivo semanal trae a todo el mundo con el mismo
     * corte—; que haya dos significa que alguien quedó afuera del último archivo y
     * sigue mostrando el número de la semana pasada.
     */
    private static ResumenDeEquipo resumir(List<FilaDeMultimetrico> filas) {
        Map<Metrica, Set<LocalDate>> cortes = new EnumMap<>(Metrica.class);
        for (FilaDeMultimetrico fila : filas) {
            fila.cortesPorMetrica().forEach((metrica, fecha) -> cortes
                    .computeIfAbsent(metrica, clave -> new LinkedHashSet<>())
                    .add(fecha));
        }
        cortes.replaceAll((metrica, fechas) -> fechas.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        return new ResumenDeEquipo(
                filas.size(),
                filas.stream().filter(FilaDeMultimetrico::completa).count(),
                filas.stream().filter(fila -> !fila.faltantes().isEmpty()).count(),
                filas.stream()
                        .map(FilaDeMultimetrico::corteMasViejo)
                        .filter(java.util.Objects::nonNull)
                        .min(Comparator.naturalOrder())
                        .orElse(null),
                cortes);
    }
}
