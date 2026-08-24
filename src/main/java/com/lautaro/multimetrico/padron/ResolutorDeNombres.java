package com.lautaro.multimetrico.padron;

import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.padron.domain.Coincidencia;
import com.lautaro.multimetrico.padron.domain.Estrategia;
import com.lautaro.multimetrico.padron.domain.NombreNormalizado;
import com.lautaro.multimetrico.padron.domain.Padron;
import com.lautaro.multimetrico.padron.domain.Pendiente;
import com.lautaro.multimetrico.padron.domain.Resolucion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Cruza los nombres que vienen en un archivo contra el padrón.
 *
 * <p>Los archivos de métricas no traen legajo: traen el nombre escrito por otro
 * sistema, cortado a un largo fijo, a veces con la codificación rota. Este es el
 * punto donde una fila deja de ser texto y pasa a ser una persona, y donde hoy se
 * pierde gente sin que nadie lo note.
 *
 * <h2>El orden importa</h2>
 *
 * Se prueban cuatro vías, de la más segura a la menos, y la primera que da
 * resultado gana:
 *
 * <ol>
 *   <li><b>Legajo</b>, si el archivo lo trae. Sin ambigüedad posible.</li>
 *   <li><b>Nombre exacto</b> ya normalizado.</li>
 *   <li><b>Alias confirmado</b>: alguien ya resolvió antes este mismo texto.</li>
 *   <li><b>Prefijo único</b>: el nombre venía cortado y es el principio de un
 *       solo asesor del padrón.</li>
 * </ol>
 *
 * <h2>Lo que deliberadamente no hace</h2>
 *
 * <p>No resuelve por parecido. Hay una medida de similitud acá adentro, pero se
 * usa <b>solo para sugerir candidatos a una persona</b>, nunca para decidir. En
 * un cálculo que termina en cuánto cobra alguien, adivinar mal significa
 * atribuirle a una persona las métricas de otra, y encima sin dejar rastro de que
 * fue una suposición. Un pendiente es barato: alguien lo confirma una vez y el
 * alias queda aprendido para siempre.
 *
 * <p>Tampoco resuelve cuando hay más de un candidato, ni siquiera si uno se
 * parece mucho más. Dos {@code PEREZ AN...} en el padrón son un empate, y un
 * empate lo desempata una persona.
 */
@Component
public class ResolutorDeNombres {

    /** Cuántos candidatos ofrecer cuando no se pudo resolver. */
    private static final int MAXIMO_SUGERENCIAS = 3;

    /**
     * Distancia máxima para molestarse en sugerir. Por encima de esto los nombres
     * no se parecen y listar candidatos sería ruido.
     */
    private static final int DISTANCIA_MAXIMA_SUGERENCIA = 5;

    private final RegistroDeAlias alias;

    public ResolutorDeNombres(RegistroDeAlias alias) {
        this.alias = alias;
    }

    public Resolucion resolver(Padron padron, List<String> nombres) {
        List<Coincidencia> resueltos = new ArrayList<>();
        List<Pendiente> pendientes = new ArrayList<>();

        for (String texto : nombres) {
            resolverUno(padron, texto).ifPresentOrElse(resueltos::add,
                    () -> pendientes.add(armarPendiente(padron, texto)));
        }
        return new Resolucion(resueltos, pendientes);
    }

    private Optional<Coincidencia> resolverUno(Padron padron, String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }

        Optional<Asesor> porLegajo = padron.porLegajo(texto);
        if (porLegajo.isPresent()) {
            return Optional.of(new Coincidencia(texto, porLegajo.get(), Estrategia.LEGAJO));
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
            return Optional.of(new Coincidencia(texto, exacto.get(), Estrategia.NOMBRE_EXACTO));
        }

        Optional<Coincidencia> porAlias = alias.legajoDe(buscado)
                .flatMap(padron::porLegajo)
                .map(asesor -> new Coincidencia(texto, asesor, Estrategia.ALIAS_CONFIRMADO));
        if (porAlias.isPresent()) {
            return porAlias;
        }

        List<Asesor> porPrefijo = padron.asesores().stream()
                .filter(asesor -> buscado.esPrefijoDe(asesor.nombreNormalizado()))
                .toList();
        if (porPrefijo.size() == 1) {
            return Optional.of(new Coincidencia(texto, porPrefijo.getFirst(), Estrategia.PREFIJO_UNICO));
        }

        return Optional.empty();
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
            return Pendiente.ambiguo(texto, ambiguos);
        }
        return Pendiente.sinCandidatos(texto, sugerir(padron, buscado));
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
