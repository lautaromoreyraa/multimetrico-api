package com.lautaro.multimetrico.service.validacion.impl;

import com.lautaro.multimetrico.domain.Banda;
import com.lautaro.multimetrico.domain.Escala;
import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.pliego.Violacion;
import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.enumeration.TipoDeViolacion;
import com.lautaro.multimetrico.service.validacion.ValidadorDePliego;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * El validador de pliegos.
 *
 * <p>Junta <b>todos</b> los problemas antes de devolver. Cargar el pliego es el
 * momento en que conviene fallar: después ya hay números calculados con reglas
 * rotas. Y quien lo carga tiene que poder arreglarlo de una pasada, no descubrir
 * el siguiente error recién después de corregir el anterior.
 */
@Service
public class ValidadorDePliegoImpl implements ValidadorDePliego {

    @Override
    public List<Violacion> validar(Pliego pliego) {
        List<Violacion> violaciones = new ArrayList<>();

        buscarMetricasDuplicadas(pliego, violaciones);
        for (Escala escala : pliego.escalas()) {
            validarEscala(escala, violaciones);
        }
        return List.copyOf(violaciones);
    }

    private void buscarMetricasDuplicadas(Pliego pliego, List<Violacion> violaciones) {
        EnumSet<Metrica> vistas = EnumSet.noneOf(Metrica.class);
        for (Escala escala : pliego.escalas()) {
            if (!vistas.add(escala.metrica())) {
                violaciones.add(new Violacion(escala.metrica(), TipoDeViolacion.METRICA_DUPLICADA,
                        "aparece más de una vez en el pliego; no hay forma de saber cuál manda"));
            }
        }
    }

    private void validarEscala(Escala escala, List<Violacion> violaciones) {
        List<Banda> bandas = escala.ordenadas();
        Metrica metrica = escala.metrica();

        if (bandas.size() < 2) {
            violaciones.add(new Violacion(metrica, TipoDeViolacion.BANDAS_INSUFICIENTES,
                    "tiene una sola banda: todos los valores pagarían lo mismo"));
            return;
        }

        validarExtremosAbiertos(bandas, metrica, violaciones);
        validarContinuidad(bandas, metrica, violaciones);
        validarPeso(escala, violaciones);
    }

    /**
     * La escala tiene que cubrir de menos infinito a más infinito.
     *
     * <p>Sin extremo abierto arriba, un FCR perfecto de 100% se cae de la tabla
     * si la mejor banda termina justo en 100. Sin extremo abierto abajo, un TMO
     * de 0 —que existe cuando todavía no hay llamadas cargadas— tampoco entra.
     */
    private void validarExtremosAbiertos(List<Banda> bandas, Metrica metrica, List<Violacion> violaciones) {
        if (!bandas.getFirst().abiertaAbajo()) {
            violaciones.add(new Violacion(metrica, TipoDeViolacion.SIN_EXTREMO_ABIERTO,
                    "la banda más baja arranca en %s: un valor por debajo no cae en ninguna banda"
                            .formatted(bandas.getFirst().desde().toPlainString())));
        }
        if (!bandas.getLast().abiertaArriba()) {
            violaciones.add(new Violacion(metrica, TipoDeViolacion.SIN_EXTREMO_ABIERTO,
                    "la banda más alta termina en %s: un valor por encima no cae en ninguna banda"
                            .formatted(bandas.getLast().hasta().toPlainString())));
        }
    }

    /**
     * Entre dos bandas consecutivas, el techo de una tiene que ser el piso de la
     * siguiente. Si sobra, hay hueco; si falta, se superponen.
     */
    private void validarContinuidad(List<Banda> bandas, Metrica metrica, List<Violacion> violaciones) {
        for (int i = 0; i < bandas.size() - 1; i++) {
            Banda actual = bandas.get(i);
            Banda siguiente = bandas.get(i + 1);

            if (actual.hasta() == null || siguiente.desde() == null) {
                // Un extremo abierto en el medio de la tabla: ya se reporta como
                // superposición contra todo lo que tenga al lado.
                violaciones.add(new Violacion(metrica, TipoDeViolacion.SUPERPOSICION,
                        "la banda %s no tiene límite en el medio de la tabla y se pisa con %s"
                                .formatted(actual.rango(), siguiente.rango())));
                continue;
            }

            int comparacion = actual.hasta().compareTo(siguiente.desde());
            if (comparacion < 0) {
                violaciones.add(new Violacion(metrica, TipoDeViolacion.HUECO,
                        "hay un hueco entre %s y %s: un valor en ese rango no cae en ninguna banda"
                                .formatted(actual.hasta().toPlainString(), siguiente.desde().toPlainString())));
            } else if (comparacion > 0) {
                violaciones.add(new Violacion(metrica, TipoDeViolacion.SUPERPOSICION,
                        "las bandas %s y %s se pisan entre %s y %s: un valor ahí cae en las dos"
                                .formatted(actual.rango(), siguiente.rango(),
                                        siguiente.desde().toPlainString(), actual.hasta().toPlainString())));
            }
        }
    }

    /**
     * El peso declarado tiene que ser lo que paga la mejor banda. Si no coinciden,
     * o el peso está mal o la tabla no llega al techo que dice el pliego, y en
     * cualquiera de los dos casos el alcance máximo que se publica es mentira.
     */
    private void validarPeso(Escala escala, List<Violacion> violaciones) {
        if (escala.peso().compareTo(escala.mejorResultado()) != 0) {
            violaciones.add(new Violacion(escala.metrica(), TipoDeViolacion.PESO_NO_COINCIDE,
                    "el peso declarado es %s pero la mejor banda paga %s"
                            .formatted(escala.peso().toPlainString(), escala.mejorResultado().toPlainString())));
        }
    }
}
