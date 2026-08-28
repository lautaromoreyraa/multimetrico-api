package com.lautaro.multimetrico.service.planilla;

import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.planilla.FilaLeida;
import java.io.InputStream;
import java.util.List;

/**
 * Traduce una planilla a filas, según lo que diga el perfil.
 *
 * <p>Es el único lugar del sistema que sabe qué es una hoja de cálculo. Todo lo
 * de adentro trabaja con {@link FilaLeida}, así que el día que la fuente publique
 * CSV —o una API— se escribe otra implementación de esta interfaz y no se toca
 * nada más.
 *
 * <p>Las columnas se ubican <b>por encabezado, no por posición</b>. Es la
 * diferencia entre un perfil que sobrevive a que agreguen una columna al principio
 * del reporte y uno que a partir de ese día importa la columna equivocada sin
 * decir nada.
 */
public interface LectorDePlanilla {

    /**
     * Las filas del archivo según el perfil.
     *
     * @throws com.lautaro.multimetrico.exception.PlanillaRechazadaException si el
     *     archivo no coincide con el perfil, con <b>todos</b> los desajustes
     */
    List<FilaLeida> leer(InputStream entrada, PerfilDeImportacion perfil);

    /**
     * Los encabezados de la hoja con algunos valores de ejemplo, columna por
     * columna.
     *
     * <p>Es lo que hace posible definir un perfil sin conocer el archivo: el
     * sistema no adivina qué columna es cuál, muestra lo que hay y deja que una
     * persona lo diga.
     *
     * @return una lista por columna, con el encabezado primero y los ejemplos
     *     después
     */
    List<List<String>> muestra(InputStream entrada, String hoja, int filaEncabezados, int ejemplos);

    List<String> hojasDe(InputStream entrada);
}
