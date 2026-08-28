package com.lautaro.multimetrico.service.validacion;

import com.lautaro.multimetrico.domain.Pliego;
import com.lautaro.multimetrico.dto.pliego.Violacion;
import java.util.List;

/**
 * Revisa que un pliego se pueda usar antes de guardarlo.
 *
 * <p>Es el corazón del proyecto y conviene ser explícito sobre por qué existe.
 * En la planilla, un pliego mal armado no se rechaza: se aplica. Una banda
 * superpuesta la resuelve la primera coincidencia; un hueco entre bandas
 * devuelve el tramo de abajo o un error de fórmula que alguien copia hacia
 * abajo sin mirar. El pliego real que originó este proyecto tiene las dos
 * cosas, y nadie se enteró hasta que se lo leyó fila por fila.
 *
 * <p><b>Devuelve una lista en vez de lanzar.</b> Quien decide qué hacer con las
 * violaciones es {@code PliegoService}. Así el validador también sirve para
 * previsualizar un pliego sin intentar guardarlo, que es el endpoint de revisión.
 */
public interface ValidadorDePliego {

    /** Todos los problemas del pliego, o una lista vacía si está sano. */
    List<Violacion> validar(Pliego pliego);
}
