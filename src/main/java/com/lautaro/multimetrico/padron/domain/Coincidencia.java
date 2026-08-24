package com.lautaro.multimetrico.padron.domain;

/** Un nombre del archivo, resuelto a una persona del padrón, y por qué vía. */
public record Coincidencia(String textoOriginal, Asesor asesor, Estrategia estrategia) {
}
