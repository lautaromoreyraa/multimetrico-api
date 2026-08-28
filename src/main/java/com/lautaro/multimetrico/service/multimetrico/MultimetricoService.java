package com.lautaro.multimetrico.service.multimetrico;

import com.lautaro.multimetrico.dto.multimetrico.FilaDeMultimetrico;
import com.lautaro.multimetrico.dto.multimetrico.Multimetrico;
import java.time.YearMonth;

/**
 * Arma el multimétrico juntando las tres piezas que hoy están sueltas: quién
 * estaba en el equipo (padrón), con qué vara se lo mide (pliego) y qué números
 * tiene (mediciones).
 *
 * <p>Las tres se piden <b>del mismo período</b>. Es la regla que hace
 * reproducible el cálculo: el multimétrico de marzo usa el padrón de marzo, el
 * pliego de marzo y las mediciones de marzo, aunque se lo pida en septiembre.
 *
 * <p>El recorrido va desde el padrón hacia las mediciones y no al revés, y eso
 * decide qué se ve. Se recorre <b>el equipo</b>, y a cada asesor se le buscan sus
 * mediciones: el que no tiene ninguna aparece igual, con todas las métricas
 * faltantes. Al revés —recorrer las mediciones— el asesor sin datos
 * desaparecería de la lista, que es justo lo que pasa hoy cuando un nombre no
 * empalma: la fila no está y nadie la extraña.
 */
public interface MultimetricoService {

    /**
     * El multimétrico de un equipo. Con {@code lider} en {@code null}, la
     * operación entera.
     *
     * <p>Un líder sin nadie a cargo es un error y no una lista vacía. Devolver
     * 200 con cero filas se lee como "tu equipo no tiene datos cargados", que
     * manda a buscar el problema al lado equivocado: el problema no es que
     * falten mediciones, es que ese líder no existe en el padrón de ese mes
     * —renunció, cambió de nombre, o el período que se pidió no es el que se
     * quería—. Por eso el mensaje trae la lista de líderes que sí están.
     */
    Multimetrico delEquipo(YearMonth periodo, String lider, String campania);

    /**
     * La fila de una sola persona: la pantalla "mi multimétrico".
     *
     * <p>Va por el padrón igual que la del equipo. Un legajo con mediciones
     * cargadas pero fuera del padrón del período no devuelve una fila suelta:
     * devuelve 404, porque una fila sin asesor no se puede mostrar —no tiene
     * nombre ni líder— y, sobre todo, porque ese legajo fuera del padrón es un
     * problema de la carga que conviene que se vea.
     */
    FilaDeMultimetrico deUnAsesor(YearMonth periodo, String legajo, String campania);
}
