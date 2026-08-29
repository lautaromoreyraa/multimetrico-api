package com.lautaro.multimetrico.service.padron;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import java.time.YearMonth;
import java.util.List;

/** Quién estaba en la operación cada mes, y cómo se traducen sus nombres a legajo. */
public interface PadronService {

    /**
     * Carga el padrón de un período, reemplazando el que hubiera.
     *
     * <p>Reemplazar es la operación normal, no una corrección excepcional: la
     * gente entra y sale dentro del mismo mes. Es la diferencia con el pliego, que
     * no se pisa nunca.
     */
    Padron cargar(Padron padron);

    Padron buscar(YearMonth periodo);

    List<YearMonth> periodos();

    /** El equipo de un líder: el filtrado que hoy se hace a mano en cada archivo. */
    List<Asesor> equipo(YearMonth periodo, String lider);

    List<String> lideres(YearMonth periodo);

    /** Suma una persona a la nómina del mes. */
    Asesor agregarAsesor(YearMonth periodo, Asesor nuevo);

    /** Corrige el nombre o el equipo de alguien. El legajo no se toca. */
    Asesor corregirAsesor(YearMonth periodo, String legajo, String nombre, String lider);

    /**
     * Saca a una persona de la nómina del mes.
     *
     * <p>Deja de aparecer en el multimétrico y en los incentivos, que se arman
     * recorriendo el padrón. Sus mediciones e importes quedan guardados: si vuelve
     * a cargarse, vuelven con ella.
     */
    Asesor quitarAsesor(YearMonth periodo, String legajo);

    /**
     * Cruza una lista de nombres contra el padrón del período.
     *
     * <p>Es una operación de solo lectura, pensada para correr antes de importar:
     * deja ver cuántas filas van a empalmar y cuáles no, sin haber guardado nada.
     */
    Resolucion resolver(YearMonth periodo, List<String> nombres);

    /**
     * Registra que un texto corresponde a un legajo.
     *
     * <p>Lo confirma una persona, y a partir de ahí el sistema no vuelve a
     * preguntar por ese texto. Se valida que el legajo exista en el padrón del
     * período: un alias que apunta a nadie es peor que no tenerlo.
     */
    Asesor confirmarAlias(YearMonth periodo, String texto, String legajo);

    List<String> aliasDe(String legajo);
}
