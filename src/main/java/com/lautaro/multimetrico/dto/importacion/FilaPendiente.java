package com.lautaro.multimetrico.dto.importacion;

import com.lautaro.multimetrico.dto.padron.Candidato;
import com.lautaro.multimetrico.dto.resolucion.Pendiente;
import com.lautaro.multimetrico.enumeration.MotivoDePendiente;
import java.math.BigDecimal;
import java.util.List;

public record FilaPendiente(
        int numeroDeFila,
        String textoAsesor,
        BigDecimal valor,
        MotivoDePendiente motivo,
        List<Candidato> candidatos) {

    public FilaPendiente {
        if (valor == null) {
            throw new IllegalArgumentException(
                    ("El pendiente de la fila %d no trae valor. Una fila sin valor es un rechazo, no un "
                            + "pendiente: no hay nada que importar cuando alguien confirme el legajo.")
                            .formatted(numeroDeFila));
        }
        candidatos = List.copyOf(candidatos == null ? List.of() : candidatos);
    }

    public static FilaPendiente de(int numeroDeFila, BigDecimal valor, Pendiente pendiente) {
        return new FilaPendiente(
                numeroDeFila,
                pendiente.textoOriginal(),
                valor,
                pendiente.motivo(),
                pendiente.candidatos());
    }

    public String explicacion() {
        return switch (motivo) {
            case AMBIGUO -> "'%s' viene cortado y es principio de más de un asesor del padrón"
                    .formatted(textoAsesor);
            case SIN_CANDIDATOS -> candidatos.isEmpty()
                    ? "'%s' no se parece a nadie del padrón del período".formatted(textoAsesor)
                    : "'%s' no coincide con nadie del padrón; estos se le parecen".formatted(textoAsesor);
        };
    }
}
