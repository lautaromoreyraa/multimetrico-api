package com.lautaro.multimetrico.service.pliego.impl;

import com.lautaro.multimetrico.domain.Banda;
import com.lautaro.multimetrico.domain.Escala;
import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.evaluacion.Alcance;
import com.lautaro.multimetrico.dto.evaluacion.Evaluacion;
import com.lautaro.multimetrico.dto.pliego.Violacion;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.exception.PliegoInvalidoException;
import com.lautaro.multimetrico.exception.PliegoYaCargadoException;
import com.lautaro.multimetrico.exception.RecursoNoEncontradoException;
import com.lautaro.multimetrico.repository.PliegoRepository;
import com.lautaro.multimetrico.service.pliego.PliegoService;
import com.lautaro.multimetrico.service.validacion.ValidadorDePliego;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El pliego: cargarlo una sola vez y aplicarlo siempre contra su propio período.
 *
 * <p>Acá vive el <b>motor de bandas</b>, que antes era un método de la entidad.
 * Es el cálculo central del sistema y por eso está en la capa de servicio: la
 * entidad sabe qué escalas tiene y en qué banda cae un valor; producir una
 * {@link Evaluacion} —decidir qué falta, qué quedó fuera de escala y cuánto suma
 * el total— es armar un resultado.
 */
@Service
public class PliegoServiceImpl implements PliegoService {

    private final PliegoRepository repositorio;
    private final ValidadorDePliego validador;

    public PliegoServiceImpl(PliegoRepository repositorio, ValidadorDePliego validador) {
        this.repositorio = repositorio;
        this.validador = validador;
    }

    @Override
    public List<Violacion> revisar(Pliego pliego) {
        return validador.validar(pliego);
    }

    /**
     * El orden de los dos controles importa.
     *
     * <p>Primero se mira si ya existe y recién después se valida: a quien vuelve a
     * subir el pliego del mes pasado hay que decirle que ya está cargado, no
     * hacerlo corregir bandas de un archivo que igual no se iba a guardar.
     */
    @Override
    @Transactional
    public Pliego cargar(Pliego pliego) {
        if (repositorio.existsByPeriodoAndCampaniaClave(pliego.periodo(), Pliego.clave(pliego.campania()))) {
            throw new PliegoYaCargadoException(pliego.periodo(), pliego.campania());
        }
        List<Violacion> violaciones = validador.validar(pliego);
        if (!violaciones.isEmpty()) {
            throw new PliegoInvalidoException(violaciones);
        }
        return repositorio.save(pliego);
    }

    @Override
    @Transactional(readOnly = true)
    public Pliego buscar(YearMonth periodo, String campania) {
        return repositorio.findByPeriodoAndCampaniaClave(periodo, Pliego.clave(campania))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay pliego cargado para %s en la campaña %s".formatted(periodo, campania)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pliego> listar() {
        return repositorio.findAllByOrderByPeriodoDescCampaniaAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Evaluacion evaluar(YearMonth periodo, String campania, Map<Metrica, BigDecimal> mediciones) {
        return evaluar(buscar(periodo, campania), mediciones);
    }

    /**
     * Evalúa las mediciones de un asesor contra un pliego.
     *
     * <p>Acepta que falten métricas. Es lo normal y no un error: cada métrica
     * llega en su propio archivo y con su propia fecha de corte, así que a mitad
     * de mes es habitual tener el TMO al 21 y el FCR al 16, y que a alguien
     * todavía no le haya llegado el NPS.
     *
     * <p>Lo que falta se informa como faltante y <b>no entra en {@code alcances}</b>,
     * que es lo que permite distinguir "no hay valor" de "hay un valor que paga
     * cero". Para el total da igual: no sumarlo es lo mismo que sumarle cero, y
     * cero es lo que una métrica sin archivo bonifica.
     *
     * <p>Un valor que no cae en ninguna banda no se tapa con un cero: se informa
     * como fuera de escala. Sobre un pliego validado no puede pasar, y si pasa es
     * que algo se guardó sin validar y hay que verlo.
     *
     * <p>Es una función pura: no toca el repositorio y no depende de cuándo se la
     * llame. Esa es la propiedad por la que recalcular marzo dentro de seis meses
     * devuelve lo mismo que devolvió en marzo.
     */
    @Override
    public Evaluacion evaluar(Pliego pliego, Map<Metrica, BigDecimal> mediciones) {
        Map<Metrica, Alcance> alcances = new EnumMap<>(Metrica.class);
        List<Metrica> faltantes = new ArrayList<>();
        List<Metrica> fueraDeEscala = new ArrayList<>();

        for (Escala escala : pliego.escalas()) {
            BigDecimal valor = mediciones.get(escala.metrica());
            if (valor == null) {
                faltantes.add(escala.metrica());
                continue;
            }
            escala.bandaPara(valor).ifPresentOrElse(
                    banda -> alcances.put(escala.metrica(), alcance(escala.metrica(), valor, banda)),
                    () -> fueraDeEscala.add(escala.metrica()));
        }

        BigDecimal total = alcances.values().stream()
                .map(Alcance::resultado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Evaluacion(
                pliego.periodo(),
                pliego.campania(),
                Map.copyOf(alcances),
                List.copyOf(faltantes),
                List.copyOf(fueraDeEscala),
                total,
                faltantes.isEmpty() && fueraDeEscala.isEmpty());
    }

    private static Alcance alcance(Metrica metrica, BigDecimal valor, Banda banda) {
        return new Alcance(metrica, valor, banda.rango(), banda.resultado());
    }
}
