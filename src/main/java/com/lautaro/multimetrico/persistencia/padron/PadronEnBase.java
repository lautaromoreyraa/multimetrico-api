package com.lautaro.multimetrico.persistencia.padron;

import com.lautaro.multimetrico.padron.PadronRepository;
import com.lautaro.multimetrico.padron.domain.Padron;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * El padrón en MySQL.
 *
 * <p>A diferencia del pliego, el padrón <b>sí se reemplaza</b>, y eso está en el
 * PLAN: es una lista de gente que cambia dentro del mes —altas, bajas, cambios
 * de equipo—, no un compromiso que haya que poder reproducir. Guardar el padrón
 * de un período borra el anterior y escribe el nuevo entero.
 */
@Repository
public class PadronEnBase implements PadronRepository {

    private final PadronesJpa padrones;

    public PadronEnBase(PadronesJpa padrones) {
        this.padrones = padrones;
    }

    @Override
    @Transactional
    public Padron guardar(Padron padron) {
        // El flush entre el borrado y el alta no es decorativo: sin él Hibernate
        // puede mandar el INSERT antes que el DELETE y la unique del período lo
        // rechaza.
        padrones.findByPeriodo(padron.periodo()).ifPresent(padrones::delete);
        padrones.flush();
        padrones.save(PadronEntidad.de(padron));
        return padron;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Padron> buscar(YearMonth periodo) {
        return padrones.findByPeriodo(periodo).map(PadronEntidad::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<YearMonth> periodos() {
        return padrones.findAllByOrderByPeriodoDesc().stream()
                .map(PadronEntidad::periodo)
                .toList();
    }
}
