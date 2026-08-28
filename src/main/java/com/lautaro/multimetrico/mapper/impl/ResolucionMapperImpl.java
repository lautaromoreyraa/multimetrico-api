package com.lautaro.multimetrico.mapper.impl;

import com.lautaro.multimetrico.dto.resolucion.Coincidencia;
import com.lautaro.multimetrico.dto.resolucion.Pendiente;
import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import com.lautaro.multimetrico.dto.resolucion.ResolucionResponse;
import com.lautaro.multimetrico.enumeration.Estrategia;
import com.lautaro.multimetrico.mapper.ResolucionMapper;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * El informe de resolución, escrito a mano.
 *
 * <p>El conteo por estrategia se calcula acá y no en el servicio porque es una
 * lectura de presentación: sirve para que quien mira el informe se dé cuenta de
 * que el archivo viene truncando nombres. No cambia ninguna decisión del sistema.
 */
@Component
public class ResolucionMapperImpl implements ResolucionMapper {

    @Override
    public ResolucionResponse aResponse(Resolucion resolucion) {
        Map<Estrategia, Long> porEstrategia = resolucion.resueltos().stream()
                .collect(Collectors.groupingBy(Coincidencia::estrategia, Collectors.counting()));

        return new ResolucionResponse(
                resolucion.resueltos().size() + resolucion.pendientes().size(),
                resolucion.resueltos().size(),
                resolucion.pendientes().size(),
                resolucion.completa(),
                porEstrategia,
                resolucion.resueltos().stream().map(ResolucionMapperImpl::coincidencia).toList(),
                resolucion.pendientes().stream().map(ResolucionMapperImpl::pendiente).toList());
    }

    private static ResolucionResponse.CoincidenciaResponse coincidencia(Coincidencia c) {
        return new ResolucionResponse.CoincidenciaResponse(
                c.textoOriginal(), c.asesor().legajo(), c.asesor().nombre(), c.estrategia());
    }

    private static ResolucionResponse.PendienteResponse pendiente(Pendiente p) {
        return new ResolucionResponse.PendienteResponse(p.textoOriginal(), motivo(p), p.candidatos());
    }

    /**
     * El motivo, redactado.
     *
     * <p>Los dos casos piden acciones distintas y el texto lo dice: al que no se
     * parece a nadie le falta un alta en el padrón, el ambiguo necesita que
     * alguien desempate. Un enum crudo obligaría a cada pantalla a redactar esto
     * por su cuenta.
     */
    private static String motivo(Pendiente pendiente) {
        return switch (pendiente.motivo()) {
            case SIN_CANDIDATOS -> pendiente.candidatos().isEmpty()
                    ? "no se parece a nadie del padrón"
                    : "no coincide con nadie; estos se le parecen";
            case AMBIGUO -> "el nombre viene cortado y coincide con más de un asesor";
        };
    }
}
