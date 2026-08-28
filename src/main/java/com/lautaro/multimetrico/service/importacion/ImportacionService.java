package com.lautaro.multimetrico.service.importacion;

import com.lautaro.multimetrico.domain.Importacion;
import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Convierte una planilla semanal en mediciones.
 *
 * <h2>La propiedad que define el módulo</h2>
 *
 * <p><b>Ninguna fila se pierde.</b> Cada fila del archivo termina en exactamente
 * uno de tres lugares: importada, rechazada con su número de fila y su motivo, o
 * pendiente de resolución con los candidatos que el padrón sugiere. La suma de
 * los tres es la cantidad de filas leídas, y {@code Informe} lo verifica al
 * construirse.
 *
 * <p>Esa es exactamente la diferencia con el proceso que reemplaza. Al pegar en
 * una planilla, la fila cuyo nombre no empalma no produce ningún síntoma: el
 * total sale igual, un poco más chico, y la persona se entera cuando reclama.
 *
 * <h2>Por qué no bloquea</h2>
 *
 * <p>La carga responde con el id de la importación apenas recibe el archivo y
 * procesa después. Un reporte con todos los equipos son decenas de miles de
 * filas, y cada una necesita resolverse contra el padrón; dejar a alguien
 * esperando frente a una pantalla en blanco —o peor, que el navegador corte por
 * timeout a mitad de camino— haría inservible la parte más valiosa, que es el
 * informe.
 */
public interface ImportacionService {

    /** Guardar un perfil con un nombre que ya existe lo edita, no lo duplica. */
    PerfilDeImportacion definirPerfil(PerfilDeImportacion perfil);

    List<PerfilDeImportacion> perfiles();

    PerfilDeImportacion perfil(String nombre);

    Importacion buscar(String id);

    List<Importacion> historial();

    /** Recibe el archivo, deja anotada la importación y devuelve el id enseguida. */
    Importacion recibir(String nombrePerfil, YearMonth periodo, LocalDate corte, byte[] archivo);

    /** Los encabezados del archivo con ejemplos, para poder armar un perfil. */
    List<List<String>> inspeccionar(byte[] archivo, String hoja, int filaEncabezados);

    List<String> hojasDe(byte[] archivo);

    /**
     * Resuelve a mano una fila que había quedado pendiente.
     *
     * <p>Se resuelve <b>por importación</b> y no como alias global, y esa es la
     * diferencia que importa. Un nombre ambiguo como {@code SOSA ANA} es prefijo
     * de dos personas: confirmarlo como alias lo ataría para siempre a un legajo,
     * y en el archivo del mes que viene podría ser la otra. Acá la decisión vale
     * para esta fila de este archivo, que es todo lo que quien la toma puede
     * saber con certeza.
     */
    Importacion resolverPendiente(String id, int numeroDeFila, String legajo);
}
