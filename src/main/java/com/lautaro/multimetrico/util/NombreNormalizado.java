package com.lautaro.multimetrico.util;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * La forma canónica de un nombre, para poder compararlo.
 *
 * <p>Los archivos de métricas no traen legajo: traen el nombre escrito, y cada
 * sistema lo escribe distinto. Comparar los textos crudos es lo que hoy hace que
 * una persona no aparezca en el multimétrico. Antes de comparar, entonces, se
 * lleva todo a la misma forma:
 *
 * <ul>
 *   <li><b>Mayúsculas</b>, porque el mismo nombre llega de las dos maneras.</li>
 *   <li><b>Sin acentos ni eñes.</b> Descomponer en NFD y sacar las marcas
 *       diacríticas convierte {@code MIÑO} en {@code MINO} y {@code GARCÍA} en
 *       {@code GARCIA}, que es lo que hace falta para que empalmen dos sistemas
 *       que no se pusieron de acuerdo en la codificación.</li>
 *   <li><b>Sin el carácter de reemplazo.</b> Cuando una planilla se guarda con la
 *       codificación equivocada, las eñes y las vocales acentuadas quedan como
 *       {@code U+FFFD}. Se lo saca, no se lo cambia por un espacio: partir el
 *       nombre en dos pedazos sería peor que acortarlo.</li>
 *   <li><b>Sin puntuación y con un solo espacio</b> entre palabras.</li>
 * </ul>
 *
 * <p>Ojo con lo que <em>no</em> hace: no reordena las palabras. En los archivos
 * de esta operación el nombre siempre viene como apellido y después nombres, así
 * que aceptar cualquier orden agregaría riesgo —{@code GOMEZ ANGELES} y
 * {@code ANGELES GOMEZ} pueden ser dos personas— sin resolver ningún caso real.
 *
 * <p>Vive en {@code util} y no en {@code domain} porque no es una entidad ni se
 * persiste: es una operación sobre texto. Lo que sí se guarda es su resultado, en
 * la columna {@code nombre} de {@code alias_de_nombre}.
 */
public record NombreNormalizado(String valor) implements Comparable<NombreNormalizado> {

    public static NombreNormalizado de(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede venir vacío");
        }
        String sinDiacriticos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String limpio = sinDiacriticos
                .replace("\uFFFD", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El nombre '%s' no deja nada al normalizarlo".formatted(texto));
        }
        return new NombreNormalizado(limpio);
    }

    /**
     * Si este nombre es el principio del otro.
     *
     * <p>Es el caso más común de todos: las planillas cortan el nombre a un largo
     * fijo y llega {@code AGUIRRE FLAVIA Y} donde el padrón dice
     * {@code AGUIRRE FLAVIA YAMILA}.
     *
     * <p><b>No se exige que el corte caiga en un borde de palabra</b>, y vale la
     * pena decir por qué, porque la intuición tira para el otro lado. Un truncado
     * a largo fijo no respeta las palabras: parte donde le toca, casi siempre a
     * mitad de un nombre. Pedir que termine en un espacio dejaría afuera
     * justamente el caso que hay que resolver.
     *
     * <p>Lo que hace segura la comparación no es dónde cae el corte sino la
     * <b>unicidad</b>, que se verifica afuera: {@code SOSA ANA} es principio de
     * {@code SOSA ANA MARIA} y también de {@code SOSA ANABELA ROSA}, así que no
     * resuelve — queda pendiente para que alguien desempate. Es el criterio
     * correcto: el problema nunca fue el borde de palabra, era el empate.
     */
    public boolean esPrefijoDe(NombreNormalizado otro) {
        return valor.length() < otro.valor().length() && otro.valor().startsWith(valor);
    }

    public List<String> palabras() {
        return List.of(valor.split(" "));
    }

    /** El apellido, que es por donde conviene buscar candidatos. */
    public String primeraPalabra() {
        return palabras().getFirst();
    }

    @Override
    public int compareTo(NombreNormalizado otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
