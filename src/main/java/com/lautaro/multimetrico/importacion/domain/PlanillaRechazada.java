package com.lautaro.multimetrico.importacion.domain;

import java.util.List;

/**
 * El archivo no coincide con su perfil y no se importa nada.
 *
 * <p>Es la conducta que distingue al sistema de la planilla. Cuando la empresa
 * cambia un encabezado o mueve una columna —y lo va a hacer—, seguir importando
 * con el perfil viejo produce números que parecen buenos y no lo son. Rechazar
 * el archivo entero es preferible: un archivo que no entró se nota, uno que
 * entró mal no.
 *
 * <p>Los desajustes van todos juntos por la misma razón que las violaciones del
 * pliego: quien arregla el perfil quiere verlos de una pasada.
 */
public class PlanillaRechazada extends RuntimeException {

    private final transient List<Desajuste> desajustes;

    public PlanillaRechazada(List<Desajuste> desajustes) {
        super("El archivo no coincide con el perfil: %d desajuste(s)".formatted(desajustes.size()));
        this.desajustes = List.copyOf(desajustes);
    }

    public List<Desajuste> desajustes() {
        return desajustes;
    }
}
