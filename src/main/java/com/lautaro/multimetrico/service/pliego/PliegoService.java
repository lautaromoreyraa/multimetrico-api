package com.lautaro.multimetrico.service.pliego;

import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.evaluacion.Evaluacion;
import com.lautaro.multimetrico.dto.pliego.Violacion;
import com.lautaro.multimetrico.enumeration.Metrica;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** Cargar, consultar y aplicar los objetivos de un mes. */
public interface PliegoService {

    /**
     * Valida sin guardar.
     *
     * <p>Existe para que el supervisor pueda ver qué tiene mal el pliego del mes
     * antes de comprometerlo.
     */
    List<Violacion> revisar(Pliego pliego);

    /**
     * Guarda un pliego nuevo.
     *
     * <p>Falla si ya hay uno para ese período y esa campaña —un pliego cargado no
     * se pisa— y si no pasa la validación.
     */
    Pliego cargar(Pliego pliego);

    Pliego buscar(YearMonth periodo, String campania);

    List<Pliego> listar();

    /**
     * Evalúa mediciones contra el pliego que regía en ese período — nunca contra
     * el vigente.
     *
     * <p>Es lo que hace que recalcular un mes viejo devuelva el mismo número que
     * devolvió entonces.
     */
    Evaluacion evaluar(YearMonth periodo, String campania, Map<Metrica, BigDecimal> mediciones);

    /**
     * Lo mismo contra un pliego que el llamador ya tiene en la mano.
     *
     * <p>Lo usa el multimétrico, que arma doscientas filas contra el mismo pliego:
     * volver a buscarlo por cada asesor sería una consulta por persona para
     * traerse siempre lo mismo. El pliego que entra tiene que ser el del período
     * que se está armando; quien lo elige es el llamador.
     */
    Evaluacion evaluar(Pliego pliego, Map<Metrica, BigDecimal> mediciones);
}
