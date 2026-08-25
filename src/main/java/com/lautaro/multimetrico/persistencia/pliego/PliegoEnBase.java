package com.lautaro.multimetrico.persistencia.pliego;

import com.lautaro.multimetrico.pliego.PliegoRepository;
import com.lautaro.multimetrico.pliego.domain.Pliego;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * El pliego en MySQL, detrás de la misma interfaz que usaba la versión en
 * memoria.
 *
 * <p>Es el único bean de {@link PliegoRepository}: la implementación en memoria
 * dejó de ser un componente de Spring y quedó como doble de test.
 */
@Repository
public class PliegoEnBase implements PliegoRepository {

    private final PliegosJpa pliegos;

    public PliegoEnBase(PliegosJpa pliegos) {
        this.pliegos = pliegos;
    }

    /**
     * Reemplaza el pliego del período y la campaña, igual que hacía el
     * {@code Map.put} de la versión en memoria.
     *
     * <p>Que acá se pise no contradice la regla de que <b>un pliego cargado no se
     * pisa</b>: esa regla la aplica {@code PliegoService}, que consulta
     * {@link #existe} y devuelve 409 antes de llegar hasta acá. El repositorio no
     * decide políticas; si además rechazara por su cuenta, habría dos lugares que
     * pueden decir que no y uno de los dos se va a olvidar.
     */
    @Override
    @Transactional
    public Pliego guardar(Pliego pliego) {
        pliegos.findByPeriodoAndCampaniaClave(pliego.periodo(), PliegoEntidad.clave(pliego.campania()))
                .ifPresent(pliegos::delete);
        pliegos.flush();
        pliegos.save(PliegoEntidad.de(pliego));
        return pliego;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pliego> buscar(YearMonth periodo, String campania) {
        return pliegos.findByPeriodoAndCampaniaClave(periodo, PliegoEntidad.clave(campania))
                .map(PliegoEntidad::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pliego> todos() {
        return pliegos.findAllByOrderByPeriodoDescCampaniaAsc().stream()
                .map(PliegoEntidad::aDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existe(YearMonth periodo, String campania) {
        return pliegos.existsByPeriodoAndCampaniaClave(periodo, PliegoEntidad.clave(campania));
    }
}
