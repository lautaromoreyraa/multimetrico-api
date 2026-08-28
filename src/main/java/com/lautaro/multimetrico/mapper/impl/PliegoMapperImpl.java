package com.lautaro.multimetrico.mapper.impl;

import com.lautaro.multimetrico.domain.Banda;
import com.lautaro.multimetrico.domain.Escala;
import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.evaluacion.Alcance;
import com.lautaro.multimetrico.dto.evaluacion.Evaluacion;
import com.lautaro.multimetrico.dto.evaluacion.EvaluacionResponse;
import com.lautaro.multimetrico.dto.pliego.PliegoRequest;
import com.lautaro.multimetrico.dto.pliego.PliegoResponse;
import com.lautaro.multimetrico.dto.pliego.Violacion;
import com.lautaro.multimetrico.enumeration.TipoDeViolacion;
import com.lautaro.multimetrico.exception.PliegoInvalidoException;
import com.lautaro.multimetrico.mapper.PliegoMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * El mapper del pliego, escrito a mano.
 *
 * <p>Escrito a mano no es una preferencia de estilo acá: {@link #aEntidad} tiene
 * lógica que ningún generador podría producir. {@link Banda} rechaza en su
 * constructor un {@code desde} mayor o igual al {@code hasta}, porque es un
 * invariante real y no debería existir una banda así ni por un instante. Pero si
 * se dejara explotar la primera, el supervisor vería un solo problema por
 * intento.
 *
 * <p>Entonces cada banda se construye por separado y las que fallan se anotan.
 * Recién si todas se pudieron construir se arma el pliego y se lo manda al
 * validador, que busca los problemas del conjunto —huecos, superposiciones,
 * pesos—. Así el invariante se mantiene y el informe sigue siendo completo.
 */
@Component
public class PliegoMapperImpl implements PliegoMapper {

    @Override
    public Pliego aEntidad(PliegoRequest request) {
        List<Violacion> problemas = new ArrayList<>();
        List<Escala> escalas = new ArrayList<>();

        for (PliegoRequest.EscalaRequest escalaRequest : request.escalas()) {
            List<Banda> bandas = new ArrayList<>();
            for (PliegoRequest.BandaRequest bandaRequest : escalaRequest.bandas()) {
                try {
                    bandas.add(new Banda(bandaRequest.desde(), bandaRequest.hasta(), bandaRequest.resultado()));
                } catch (IllegalArgumentException e) {
                    problemas.add(new Violacion(escalaRequest.metrica(), TipoDeViolacion.SUPERPOSICION,
                            "banda inválida: " + e.getMessage()));
                }
            }
            if (bandas.size() == escalaRequest.bandas().size()) {
                escalas.add(new Escala(escalaRequest.metrica(), escalaRequest.peso(), bandas));
            }
        }

        if (!problemas.isEmpty()) {
            throw new PliegoInvalidoException(problemas);
        }
        return new Pliego(request.periodo(), request.campania(), escalas);
    }

    @Override
    public PliegoResponse aResponse(Pliego pliego) {
        return new PliegoResponse(
                pliego.periodo(),
                pliego.campania(),
                pliego.alcanceMaximo(),
                pliego.escalas().stream().map(PliegoMapperImpl::escala).toList());
    }

    @Override
    public List<PliegoResponse> aResponses(List<Pliego> pliegos) {
        return pliegos.stream().map(this::aResponse).toList();
    }

    @Override
    public EvaluacionResponse aResponse(Evaluacion evaluacion) {
        List<EvaluacionResponse.AlcanceResponse> alcances = evaluacion.alcances().values().stream()
                .map(PliegoMapperImpl::alcance)
                .sorted(Comparator.comparing(a -> a.metrica().name()))
                .toList();

        return new EvaluacionResponse(
                evaluacion.periodo(),
                evaluacion.campania(),
                evaluacion.alcanceTotal(),
                evaluacion.completa(),
                alcances,
                evaluacion.faltantes(),
                evaluacion.fueraDeEscala());
    }

    private static PliegoResponse.EscalaResponse escala(Escala escala) {
        return new PliegoResponse.EscalaResponse(
                escala.metrica(),
                escala.metrica().descripcion(),
                escala.metrica().sentido().name(),
                escala.metrica().unidad(),
                escala.peso(),
                // Se devuelven ordenadas: el que las lee espera una tabla, no el
                // orden en que quedaron cargadas.
                escala.ordenadas().stream()
                        .map(b -> new PliegoResponse.BandaResponse(b.desde(), b.hasta(), b.resultado(), b.rango()))
                        .toList());
    }

    private static EvaluacionResponse.AlcanceResponse alcance(Alcance alcance) {
        return new EvaluacionResponse.AlcanceResponse(
                alcance.metrica(),
                alcance.valor(),
                alcance.banda(),
                alcance.resultado());
    }
}
