package com.lautaro.multimetrico.mapper.impl;

import com.lautaro.multimetrico.dto.multimetrico.CeldaDeMultimetrico;
import com.lautaro.multimetrico.dto.multimetrico.FilaDeMultimetrico;
import com.lautaro.multimetrico.dto.multimetrico.FilaResponse;
import com.lautaro.multimetrico.dto.multimetrico.Multimetrico;
import com.lautaro.multimetrico.dto.multimetrico.MultimetricoResponse;
import com.lautaro.multimetrico.dto.multimetrico.TramoDeArriba;
import com.lautaro.multimetrico.mapper.MultimetricoMapper;
import org.springframework.stereotype.Component;

/**
 * El mapper del multimétrico, escrito a mano.
 *
 * <p>Tiene una decisión propia y por eso no podría generarse: la frase del tramo
 * de arriba. Se arma acá y no en cada pantalla porque es la lectura correcta de
 * la condición —alcanzar el borde alcanza, bajar de él exige cruzarlo— y dejarla
 * librada al frontend es garantizar que en algún lado diga "te faltan 0" sin
 * explicar por qué el asesor no está en la banda de arriba.
 */
@Component
public class MultimetricoMapperImpl implements MultimetricoMapper {

    @Override
    public MultimetricoResponse aResponse(Multimetrico multimetrico) {
        return new MultimetricoResponse(
                multimetrico.periodo(),
                multimetrico.campania(),
                multimetrico.lider(),
                multimetrico.alcanceMaximo(),
                multimetrico.resumen(),
                multimetrico.filas().stream().map(this::aResponse).toList());
    }

    @Override
    public FilaResponse aResponse(FilaDeMultimetrico fila) {
        return new FilaResponse(
                fila.asesor().legajo(),
                fila.asesor().nombre(),
                fila.asesor().lider(),
                fila.alcanceTotal(),
                fila.completa(),
                fila.corteMasViejo(),
                fila.celdas().stream().map(MultimetricoMapperImpl::celda).toList(),
                fila.faltantes(),
                fila.fueraDeEscala());
    }

    private static FilaResponse.CeldaResponse celda(CeldaDeMultimetrico celda) {
        return new FilaResponse.CeldaResponse(
                celda.metrica(),
                celda.metrica().descripcion(),
                celda.metrica().sentido(),
                celda.metrica().unidad(),
                celda.alcance().valor(),
                celda.alcance().banda(),
                celda.alcance().resultado(),
                celda.actualizadoAl(),
                celda.tramoDeArriba().map(MultimetricoMapperImpl::tramo).orElse(null));
    }

    private static FilaResponse.TramoResponse tramo(TramoDeArriba tramo) {
        String detalle = switch (tramo.condicion()) {
            case ALCANZAR -> "faltan %s para llegar a %s y sumar %s".formatted(
                    tramo.distancia().toPlainString(),
                    tramo.objetivo().toPlainString(),
                    tramo.mejora().toPlainString());
            case BAJAR_DE -> "hay que quedar por debajo de %s —sobran %s— y sumar %s".formatted(
                    tramo.objetivo().toPlainString(),
                    tramo.distancia().toPlainString(),
                    tramo.mejora().toPlainString());
        };

        return new FilaResponse.TramoResponse(
                tramo.banda(),
                tramo.objetivo(),
                tramo.distancia(),
                tramo.condicion(),
                tramo.resultado(),
                tramo.mejora(),
                detalle);
    }
}
