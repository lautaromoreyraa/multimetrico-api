package com.lautaro.multimetrico.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public record NombreNormalizado(String valor) implements Comparable<NombreNormalizado> {

    public static NombreNormalizado de(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede venir vacío");
        }
        String sinDiacriticos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String limpio = sinDiacriticos
                .replace("\uFFFD", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El nombre '%s' no deja nada al normalizarlo".formatted(texto));
        }
        return new NombreNormalizado(limpio);
    }

    public boolean esPrefijoDe(NombreNormalizado otro) {
        return valor.length() < otro.valor().length() && otro.valor().startsWith(valor);
    }

    public List<String> palabras() {
        return List.of(valor.split(" "));
    }

    public String primeraPalabra() {
        return palabras().getFirst();
    }

    @Override
    public int compareTo(NombreNormalizado otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
