package com.lautaro.multimetrico.pliego;

import com.lautaro.multimetrico.pliego.domain.Evaluacion;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import com.lautaro.multimetrico.pliego.domain.Pliego;
import com.lautaro.multimetrico.pliego.validacion.PliegoInvalidoException;
import com.lautaro.multimetrico.pliego.validacion.ValidadorDePliego;
import com.lautaro.multimetrico.pliego.validacion.Violacion;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class PliegoService {

    private final PliegoRepository repositorio;
    private final ValidadorDePliego validador;

    public PliegoService(PliegoRepository repositorio, ValidadorDePliego validador) {
        this.repositorio = repositorio;
        this.validador = validador;
    }

    /**
     * Valida sin guardar. Existe para que el supervisor pueda ver qué tiene mal
     * el pliego del mes antes de comprometerlo.
     */
    public List<Violacion> revisar(Pliego pliego) {
        return validador.validar(pliego);
    }

    public Pliego cargar(Pliego pliego) {
        if (repositorio.existe(pliego.periodo(), pliego.campania())) {
            throw new PliegoYaCargadoException(pliego.periodo(), pliego.campania());
        }
        List<Violacion> violaciones = validador.validar(pliego);
        if (!violaciones.isEmpty()) {
            throw new PliegoInvalidoException(violaciones);
        }
        return repositorio.guardar(pliego);
    }

    public Pliego buscar(YearMonth periodo, String campania) {
        return repositorio.buscar(periodo, campania)
                .orElseThrow(() -> new NoSuchElementException(
                        "No hay pliego cargado para %s en la campaña %s".formatted(periodo, campania)));
    }

    public List<Pliego> listar() {
        return repositorio.todos();
    }

    /**
     * Evalúa mediciones contra el pliego que regía en ese período — nunca contra
     * el vigente. Es lo que hace que recalcular un mes viejo devuelva el mismo
     * número que devolvió entonces.
     */
    public Evaluacion evaluar(YearMonth periodo, String campania, Map<Metrica, BigDecimal> mediciones) {
        return buscar(periodo, campania).evaluar(mediciones);
    }
}
