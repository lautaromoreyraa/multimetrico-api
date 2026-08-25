package com.lautaro.multimetrico.importacion;

import com.lautaro.multimetrico.importacion.domain.PerfilDeImportacion;
import java.util.List;
import java.util.Optional;

/**
 * Los perfiles guardados, uno por fuente.
 *
 * <p>El perfil es <b>dato, no código</b>, y esta interfaz es lo que hace cierta
 * esa frase. Cuando la empresa cambie el reporte, se edita un registro; no se
 * recompila ni se redespliega nada.
 */
public interface PerfilRepository {

    PerfilDeImportacion guardar(PerfilDeImportacion perfil);

    Optional<PerfilDeImportacion> porNombre(String nombre);

    List<PerfilDeImportacion> todos();
}
