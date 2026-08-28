package com.lautaro.multimetrico.service.resolucion.impl;

import com.lautaro.multimetrico.domain.AliasDeNombre;
import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.resolucion.Coincidencia;
import com.lautaro.multimetrico.dto.resolucion.Pendiente;
import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import com.lautaro.multimetrico.enumeration.Estrategia;
import com.lautaro.multimetrico.mapper.PadronMapper;
import com.lautaro.multimetrico.repository.AliasRepository;
import com.lautaro.multimetrico.service.resolucion.ResolutorDeNombres;
import com.lautaro.multimetrico.util.NombreNormalizado;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** La resolución de nombres contra el padrón. El criterio está en la interfaz. */
@Service
public class ResolutorDeNombresImpl implements ResolutorDeNombres {

    /** Cuántos candidatos ofrecer cuando no se pudo resolver. */
    private static final int MAXIMO_SUGERENCIAS = 3;

    /**
     * Distancia máxima para molestarse en sugerir. Por encima de esto los nombres
     * no se parecen y listar candidatos sería ruido.
     */
    private static final int DISTANCIA_MAXIMA_SUGERENCIA = 5;

    private final AliasRepository alias;
    private final PadronMapper mapper;

    public ResolutorDeNombresImpl(AliasRepository alias, PadronMapper mapper) {
        this.alias = alias;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Resolucion resolver(Padron padron, List<String> nombres) {
        List<Coincidencia> resueltos = new ArrayList<>();
        List<Pendiente> pendientes = new ArrayList<>();

        for (String texto : nombres) {
            resolverUno(padron, texto).ifPresentOrElse(resueltos::add,
                    () -> pendientes.add(armarPendiente(padron, texto)));
        }
        return Resolucion.de(resueltos, pendientes);
    }

    private Optional<Coincidencia> resolverUno(Padron padron, String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }

        Optional<Asesor> porLegajo = padron.porLegajo(texto);
        if (porLegajo.isPresent()) {
            return Optional.of(coincidencia(texto, porLegajo.get(), Estrategia.LEGAJO));
        }

        NombreNormalizado buscado;
        try {
            buscado = NombreNormalizado.de(texto);
        } catch (IllegalArgumentException e) {
            // Un texto que no deja nada al normalizar (solo símbolos, o el
            // encabezado de la planilla colado como fila) no es un nombre.
            return Optional.empty();
        }

        Optional<Asesor> exacto = padron.asesores().stream()
                .filter(asesor -> asesor.nombreNormalizado().equals(buscado))
                .findFirst();
        if (exacto.isPresent()) {
            return Optional.of(coincidencia(texto, exacto.get(), Estrategia.NOMBRE_EXACTO));
        }

        Optional<Coincidencia> porAlias = alias.findByNombre(buscado.valor())
                .map(AliasDeNombre::legajo)
                .flatMap(padron::porLegajo)
                .map(asesor -> coincidencia(texto, asesor, Estrategia.ALIAS_CONFIRMADO));
        if (porAlias.isPresent()) {
            return porAlias;
        }

        List<Asesor> porPrefijo = padron.asesores().stream()
                .filter(asesor -> buscado.esPrefijoDe(asesor.nombreNormalizado()))
                .toList();
        if (porPrefijo.size() == 1) {
            return Optional.of(coincidencia(texto, porPrefijo.getFirst(), Estrategia.PREFIJO_UNICO));
        }

        return Optional.empty();
    }

    private Coincidencia coincidencia(String texto, Asesor asesor, Estrategia estrategia) {
        return new Coincidencia(texto, mapper.aResponse(asesor), estrategia);
    }

    private Pendiente armarPendiente(Padron padron, String texto) {
        NombreNormalizado buscado;
        try {
            buscado = NombreNormalizado.de(texto);
        } catch (IllegalArgumentException e) {
            return Pendiente.sinCandidatos(texto, List.of());
        }

        List<Asesor> ambiguos = padron.asesores().stream()
                .filter(asesor -> buscado.esPrefijoDe(asesor.nombreNormalizado()))
                .toList();
        if (ambiguos.size() > 1) {
            return Pendiente.ambiguo(texto, mapper.aCandidatos(ambiguos));
        }
        return Pendiente.sinCandidatos(texto, mapper.aCandidatos(sugerir(padron, buscado)));
    }

    /**
     * Candidatos ordenados por parecido, para que una persona elija.
     *
     * <p>Se compara contra el nombre entero y no solo contra el apellido: cuando
     * la codificación rompe una eñe, el daño suele estar justo ahí.
     */
    private List<Asesor> sugerir(Padron padron, NombreNormalizado buscado) {
        return padron.asesores().stream()
                .map(asesor -> new Object() {
                    final Asesor candidato = asesor;
                    final int distancia = distancia(buscado.valor(), asesor.nombreNormalizado().valor());
                })
                .filter(par -> par.distancia <= DISTANCIA_MAXIMA_SUGERENCIA)
                .sorted(Comparator.comparingInt(par -> par.distancia))
                .limit(MAXIMO_SUGERENCIAS)
                .map(par -> par.candidato)
                .toList();
    }

    /**
     * Distancia de edición entre dos textos: cuántas inserciones, borrados o
     * sustituciones hacen falta para convertir uno en el otro.
     *
     * <p>Solo para ordenar sugerencias. Nunca decide una resolución.
     */
    private static int distancia(String a, String b) {
        int[] anterior = new int[b.length() + 1];
        int[] actual = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) {
            anterior[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            actual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                actual[j] = Math.min(Math.min(actual[j - 1] + 1, anterior[j] + 1), anterior[j - 1] + costo);
            }
            int[] intercambio = anterior;
            anterior = actual;
            actual = intercambio;
        }
        return anterior[b.length()];
    }
}
