package com.lautaro.multimetrico.dto.importacion;

/**
 * Una fila que sí se importó, pero cuyo líder en el archivo no es el del padrón.
 *
 * <p><b>No es un cuarto estado.</b> La fila entró; esto es una anotación al
 * margen. La cuenta que define el módulo —importadas + rechazadas + pendientes
 * igual a filas leídas— la sigue cumpliendo, y {@link Informe} la verifica.
 *
 * <p>Se anota y no se rechaza porque el padrón es la fuente de verdad del
 * líder: los archivos de métricas se generan con una foto vieja de la nómina y
 * un pase de equipo tarda semanas en llegarles. Rechazar por eso perdería filas
 * legítimas, que es el defecto que este módulo existe para no repetir.
 *
 * <p>Pero tampoco se calla, y esa es la parte que importa: cuando el nombre se
 * resolvió por prefijo único —porque venía cortado— el líder del archivo es la
 * única evidencia independiente de que se resolvió a la persona correcta. Dos
 * apellidos parecidos en equipos distintos aparecen acá antes de que alguien
 * reclame un número que no es suyo.
 */
public record DiscrepanciaDeLider(
        int numeroDeFila,
        String textoAsesor,
        String legajo,
        String liderDelArchivo,
        String liderDelPadron) {

    public String explicacion() {
        return ("la fila %d dice que '%s' es del equipo de '%s', pero en el padrón el legajo %s está "
                + "con '%s'; se importó con el padrón")
                .formatted(numeroDeFila, textoAsesor, liderDelArchivo, legajo, liderDelPadron);
    }
}
