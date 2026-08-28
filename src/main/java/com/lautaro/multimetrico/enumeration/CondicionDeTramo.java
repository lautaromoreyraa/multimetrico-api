package com.lautaro.multimetrico.enumeration;

/**
 * Qué hay que hacer con el objetivo para entrar en la banda de arriba.
 *
 * <p>Las bandas son intervalos semiabiertos {@code [desde, hasta)} y por eso los
 * dos bordes no se alcanzan igual: subiendo, llegar justo al borde ya cuenta;
 * bajando, el borde no entra y hay que cruzarlo.
 */
public enum CondicionDeTramo {

    /** Llegar al objetivo o pasarlo. El borde de abajo de la banda entra. */
    ALCANZAR,

    /** Quedar estrictamente por debajo del objetivo. El borde de arriba no entra. */
    BAJAR_DE
}
