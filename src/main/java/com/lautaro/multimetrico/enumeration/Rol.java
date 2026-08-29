package com.lautaro.multimetrico.enumeration;

/**
 * Qué puede ver y hacer un usuario.
 *
 * <p>Son tres y no más porque son las tres relaciones distintas que la operación
 * tiene con el multimétrico: el que aparece en una fila, el que responde por un
 * equipo, y el que carga los datos del mes.
 *
 * <p><b>El rol no alcanza para autorizar.</b> Decide a qué endpoints se llega,
 * pero no de quién es la fila que se está pidiendo: dos supervisores tienen el
 * mismo rol y equipos distintos. Esa segunda mitad la resuelve
 * {@code ControlDeAcceso}, y es la que importa — el requisito del proyecto es que
 * un asesor que pide la fila de otro reciba 403, y eso no lo puede contestar un
 * enum.
 *
 * <p>Se persiste con {@code EnumType.STRING}, igual que todos los enums del
 * proyecto: el ordinal cambia de significado si alguien agrega un rol en el medio
 * de la lista, y ahí un ASESOR pasaría a ser ADMIN sin que nada avise.
 */
public enum Rol {

    /** Ve su propia fila y nada más. */
    ASESOR,

    /** Ve las filas de su equipo. Quién es su equipo lo dice {@code Usuario.lider}. */
    SUPERVISOR,

    /** Ve todo y es el único que carga pliegos, padrones y mediciones. */
    ADMIN;

    /**
     * El nombre con el que Spring Security espera encontrar el rol.
     *
     * <p>El prefijo {@code ROLE_} es una convención de la librería, no del
     * dominio: {@code hasRole("ADMIN")} busca internamente la autoridad
     * {@code ROLE_ADMIN}. Se arma acá y no en cada lugar que lo necesita para que
     * el dominio no tenga que saber de esa convención más de una vez.
     */
    public String autoridad() {
        return "ROLE_" + name();
    }
}
