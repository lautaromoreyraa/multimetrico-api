package com.lautaro.multimetrico.util;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.util.List;

public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    public static String escribir(List<?> lista) {
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(lista);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el informe de la importación", e);
        }
    }

    public static <T> List<T> leer(String json, TypeReference<List<T>> tipo) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, tipo);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer el informe de la importación", e);
        }
    }
}
