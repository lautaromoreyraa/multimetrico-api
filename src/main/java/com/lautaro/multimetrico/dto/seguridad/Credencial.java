package com.lautaro.multimetrico.dto.seguridad;

import com.lautaro.multimetrico.enumeration.Rol;

/**
 * Quién está pidiendo, según el token.
 *
 * <p>Es lo que el filtro deja en el contexto de seguridad y lo que
 * {@code ControlDeAcceso} lee para decidir. Tiene exactamente los tres datos que
 * hacen falta para autorizar y ninguno más: el legajo dice de quién es la fila
 * propia, el rol dice a qué se llega, y el líder dice cuál es el equipo.
 *
 * <p><b>Sale del token, no de la base.</b> Eso lo hace rápido —no hay una consulta
 * por request— y tiene la contracara que conviene decir en voz alta: si a alguien
 * le cambian el rol o el equipo, su token viejo sigue diciendo lo anterior hasta
 * que vence. Con ocho horas de vigencia es un riesgo asumido; si algún día hace
 * falta revocar en el momento, esto pasa a leerse de la base y se paga la
 * consulta.
 */
public record Credencial(String legajo, Rol rol, String lider) {

    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }

    public boolean esSupervisor() {
        return rol == Rol.SUPERVISOR;
    }
}
