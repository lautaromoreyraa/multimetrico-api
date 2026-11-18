package com.lautaro.multimetrico.exception;

import com.lautaro.multimetrico.dto.importacion.Desajuste;
import java.util.List;

public class PlanillaRechazadaException extends RuntimeException {

    private final transient List<Desajuste> desajustes;

    public PlanillaRechazadaException(List<Desajuste> desajustes) {
        super("El archivo no coincide con el perfil: %d desajuste(s)".formatted(desajustes.size()));
        this.desajustes = List.copyOf(desajustes);
    }

    public List<Desajuste> desajustes() {
        return desajustes;
    }
}
