package com.lautaro.multimetrico.importacion.domain;

import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.padron.domain.Pendiente;
import java.math.BigDecimal;
import java.util.List;

/**
 * Una fila que está bien salvo por una cosa: no se sabe de quién es.
 *
 * <p>Es distinta de un rechazo y por eso es un estado propio. En un rechazo no
 * hay nada que hacer con la fila; acá el número es válido y está esperando que
 * una persona diga a qué legajo corresponde el nombre. Por eso el
 * {@code valor} viaja <b>ya interpretado</b>: cuando alguien confirme el legajo,
 * la medición se crea sin volver a abrir el archivo, que a esa altura ya no
 * está.
 *
 * <p>Los {@code candidatos} son los que el resolutor sugiere. Nunca se elige uno
 * solo: si el sistema pudiera decidir, la fila no sería un pendiente.
 */
public record FilaPendiente(
        int numeroDeFila,
        String textoAsesor,
        BigDecimal valor,
        Motivo motivo,
        List<Candidato> candidatos) {

    /** Un asesor del padrón ofrecido para que una persona lo confirme. */
    public record Candidato(String legajo, String nombre) {

        public static Candidato de(Asesor asesor) {
            return new Candidato(asesor.legajo(), asesor.nombre());
        }
    }

    /**
     * Por qué quedó sin resolver.
     *
     * <p>Es el mismo par que devuelve el padrón, repetido acá a propósito: el
     * informe de una importación no debería obligar a quien lo lee a conocer los
     * tipos del módulo de al lado, y los dos motivos piden acciones distintas.
     * El {@link #SIN_CANDIDATOS} suele ser un alta que falta en el padrón; el
     * {@link #AMBIGUO} necesita que alguien desempate.
     */
    public enum Motivo {

        SIN_CANDIDATOS,
        AMBIGUO;

        public static Motivo de(Pendiente.Motivo motivo) {
            return motivo == Pendiente.Motivo.AMBIGUO ? AMBIGUO : SIN_CANDIDATOS;
        }
    }

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
                Motivo.de(pendiente.motivo()),
                pendiente.candidatos().stream().map(Candidato::de).toList());
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
