package com.lautaro.multimetrico.pliego.web;

import com.lautaro.multimetrico.pliego.domain.Banda;
import com.lautaro.multimetrico.pliego.domain.Escala;
import com.lautaro.multimetrico.pliego.domain.Pliego;
import com.lautaro.multimetrico.pliego.validacion.PliegoInvalidoException;
import com.lautaro.multimetrico.pliego.validacion.Violacion;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Traduce el pliego de JSON al dominio.
 *
 * <p>Tiene una responsabilidad menos obvia: recolectar los errores de forma que
 * impiden construir el objeto. {@link Banda} rechaza en su constructor un
 * {@code desde} mayor o igual al {@code hasta}, porque es un invariante real y
 * no debería existir una banda así ni por un instante. Pero si se dejara
 * explotar la primera, el supervisor vería un solo problema por intento.
 *
 * <p>Entonces cada banda se construye por separado y las que fallan se anotan.
 * Recién si todas se pudieron construir se arma el pliego y se lo manda al
 * validador, que busca los problemas del conjunto —huecos, superposiciones,
 * pesos—. Así el invariante se mantiene y el informe sigue siendo completo.
 */
@Component
public class PliegoMapper {

    public Pliego aDominio(PliegoRequest request) {
        List<Violacion> problemas = new ArrayList<>();
        List<Escala> escalas = new ArrayList<>();

        for (PliegoRequest.EscalaRequest escalaRequest : request.escalas()) {
            List<Banda> bandas = new ArrayList<>();
            for (PliegoRequest.BandaRequest bandaRequest : escalaRequest.bandas()) {
                try {
                    bandas.add(new Banda(bandaRequest.desde(), bandaRequest.hasta(), bandaRequest.resultado()));
                } catch (IllegalArgumentException e) {
                    problemas.add(new Violacion(escalaRequest.metrica(), Violacion.Tipo.SUPERPOSICION,
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
}
