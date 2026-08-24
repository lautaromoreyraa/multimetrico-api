package com.lautaro.multimetrico.pliego.domain;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * La tabla de bandas de una métrica dentro de un pliego, con su peso.
 *
 * <p>El peso es el techo: lo máximo que esa métrica puede aportar al alcance
 * total. En la planilla viaja metido en el título de la tabla —"NPS (3,5%)"—,
 * acá es un campo, porque es un dato del negocio y no un rótulo.
 *
 * <p>Este record no valida que las bandas sean coherentes. De eso se encarga
 * {@code ValidadorDePliego}, y la razón es práctica: quien carga un pliego
 * quiere ver <em>todos</em> los problemas de una vez, no el primero que
 * aparezca. Un constructor que revienta sirve para proteger un invariante, no
 * para redactar un informe.
 */
public record Escala(Metrica metrica, BigDecimal peso, List<Banda> bandas) {

    public Escala {
        if (metrica == null) {
            throw new IllegalArgumentException("La escala necesita una métrica");
        }
        if (peso == null) {
            throw new IllegalArgumentException("La escala de %s necesita un peso".formatted(metrica));
        }
        if (bandas == null || bandas.isEmpty()) {
            throw new IllegalArgumentException("La escala de %s no tiene bandas".formatted(metrica));
        }
        bandas = List.copyOf(bandas);
    }

    /**
     * Las bandas ordenadas de menor a mayor. La abierta hacia abajo va primero
     * y la abierta hacia arriba, última.
     */
    public List<Banda> ordenadas() {
        return bandas.stream()
                .sorted(Comparator.comparing(Banda::desde, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * En qué banda cae un valor.
     *
     * <p>Devuelve {@link Optional} y no una banda por defecto a propósito: si un
     * valor no cae en ninguna, eso es un pliego mal armado y hay que verlo, no
     * taparlo con un cero. Sobre un pliego validado nunca está vacío.
     */
    public Optional<Banda> bandaPara(BigDecimal valor) {
        return bandas.stream().filter(banda -> banda.contiene(valor)).findFirst();
    }

    /** El mejor resultado que ofrece la tabla; tiene que coincidir con el peso. */
    public BigDecimal mejorResultado() {
        return bandas.stream()
                .map(Banda::resultado)
                .max(BigDecimal::compareTo)
                .orElseThrow();
    }
}
