package com.lautaro.multimetrico.service.resolucion;

import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import java.util.List;

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
 * <p>No resuelve por parecido. Hay una medida de similitud en la implementación,
 * pero se usa <b>solo para sugerir candidatos a una persona</b>, nunca para
 * decidir. En un cálculo que termina en cuánto cobra alguien, adivinar mal
 * significa atribuirle a una persona las métricas de otra, y encima sin dejar
 * rastro de que fue una suposición. Un pendiente es barato: alguien lo confirma
 * una vez y el alias queda aprendido para siempre.
 *
 * <p>Tampoco resuelve cuando hay más de un candidato, ni siquiera si uno se
 * parece mucho más. Dos {@code PEREZ AN...} en el padrón son un empate, y un
 * empate lo desempata una persona.
 *
 * <p>Recibe el padrón <b>entero</b> y no lo busca: la decisión de si un nombre
 * cortado apunta a una sola persona o a varias no se puede tomar mirando una fila
 * aislada.
 */
public interface ResolutorDeNombres {

    Resolucion resolver(Padron padron, List<String> nombres);
}
