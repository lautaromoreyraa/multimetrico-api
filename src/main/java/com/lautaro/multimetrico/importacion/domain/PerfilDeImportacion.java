package com.lautaro.multimetrico.importacion.domain;

import com.lautaro.multimetrico.pliego.domain.Metrica;

/**
 * Dónde está cada cosa dentro de una planilla de origen.
 *
 * <p><b>Nadie de este proyecto tiene acceso a esos archivos, ni los va a tener.</b>
 * No se sabe qué columnas traen, en qué orden, con qué encabezados ni en qué
 * hoja. Y aunque se supiera, la empresa que los publica los cambia sin avisar.
 * Escribir el mapeo en el código significaría que cada cambio de encabezado es
 * un release; escribirlo acá significa que es una fila en una tabla de
 * configuración. Esa es la decisión que sostiene el módulo entero.
 *
 * <h2>Las columnas se nombran por su encabezado, no por su posición</h2>
 *
 * <p>Podría guardarse el número de columna, que es lo que la inspección muestra
 * primero. No se hace, porque una posición no es una identidad: el día que la
 * empresa inserte una columna al principio, todas las demás se corren y un
 * perfil por posición seguiría importando —leyendo la columna de al lado— sin
 * que nada falle. Eso es exactamente lo que hace la planilla que este proyecto
 * reemplaza. Con el encabezado como identidad, una columna que se mueve se
 * vuelve a encontrar sola, y una que se renombra frena la importación entera.
 *
 * <p>El costo de la decisión, dicho para que no sorprenda: una columna
 * <b>sin encabezado</b> no se puede referenciar. Hoy no hay forma de perfilarla.
 *
 * <h2>Detalles que se pagan caro si se invierten</h2>
 *
 * <ul>
 *   <li>{@code filaEncabezados} va <b>en base 1, como la numera Excel</b>. Todo
 *       número de fila que sale de este módulo —sobre todo el de un rechazo—
 *       tiene que poder tipearse en el cuadro de "ir a" de Excel y caer en la
 *       fila que el mensaje describe. Un rechazo cuyo número está corrido en uno
 *       hace perder más tiempo que no informarlo.</li>
 *   <li>{@code hoja} en {@code null} es "la primera". Los archivos de una sola
 *       hoja son la mayoría y obligarlos a nombrarla no aporta nada.</li>
 *   <li>{@code columnaLider} es opcional. Cuando está, no decide nada: el líder
 *       de cada asesor lo pone el padrón. Sirve como control cruzado — si el
 *       archivo dice un líder y el padrón dice otro, hay algo para mirar.</li>
 *   <li>{@code columnaAsesor} puede traer el legajo en vez del nombre. No hace
 *       falta un campo aparte: el resolutor prueba primero por legajo, así que
 *       una columna que traiga legajos resuelve por la vía más segura sin que el
 *       perfil tenga que declararlo.</li>
 * </ul>
 *
 * <p>Un perfil se puede reemplazar cargándolo de nuevo con el mismo nombre, como
 * el padrón y a diferencia del pliego. Es configuración de trabajo: se corrige
 * hasta que el archivo entra. Que reemplazarlo no reescriba la historia lo
 * garantiza {@link Importacion}, que se queda con una copia del perfil con el
 * que efectivamente se procesó.
 */
public record PerfilDeImportacion(
        String nombre,
        Metrica metrica,
        String hoja,
        int filaEncabezados,
        String columnaAsesor,
        String columnaValor,
        String columnaLider,
        FormatoDeNumero formatoNumero) {

    public PerfilDeImportacion {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El perfil necesita un nombre");
        }
        if (metrica == null) {
            throw new IllegalArgumentException(
                    "El perfil %s necesita decir qué métrica trae el archivo".formatted(nombre));
        }
        if (filaEncabezados < 1) {
            throw new IllegalArgumentException(
                    ("La fila de encabezados del perfil %s se numera como en Excel, desde 1; "
                            + "llegó %d").formatted(nombre, filaEncabezados));
        }
        if (columnaAsesor == null || columnaAsesor.isBlank()) {
            throw new IllegalArgumentException(
                    "El perfil %s necesita el encabezado de la columna del asesor".formatted(nombre));
        }
        if (columnaValor == null || columnaValor.isBlank()) {
            throw new IllegalArgumentException(
                    "El perfil %s necesita el encabezado de la columna del valor".formatted(nombre));
        }
        nombre = nombre.trim();
        hoja = vacioComoNulo(hoja);
        columnaAsesor = columnaAsesor.trim();
        columnaValor = columnaValor.trim();
        columnaLider = vacioComoNulo(columnaLider);
        formatoNumero = formatoNumero == null ? FormatoDeNumero.estandar() : formatoNumero;
    }

    public boolean traeLider() {
        return columnaLider != null;
    }

    /** La primera hoja del libro cuando el perfil no nombra ninguna. */
    public boolean cualquierHoja() {
        return hoja == null;
    }

    /** El índice de la fila de encabezados como lo cuenta POI, en base 0. */
    public int filaEncabezadosEnPoi() {
        return filaEncabezados - 1;
    }

    private static String vacioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
