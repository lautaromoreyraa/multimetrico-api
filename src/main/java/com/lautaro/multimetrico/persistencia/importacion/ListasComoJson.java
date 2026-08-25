package com.lautaro.multimetrico.persistencia.importacion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/**
 * Serializa las listas del informe de una importación a una columna de texto.
 *
 * <h2>Por qué no son cuatro tablas</h2>
 *
 * <p>Rechazos, pendientes, discrepancias y desajustes son las listas de un
 * <b>reporte</b>: se escriben una vez, se leen siempre enteras y siempre de una
 * sola importación. Nadie pregunta "todos los rechazos por valor no numérico del
 * año", y si algún día hiciera falta, sería un reporte nuevo y no una consulta
 * sobre estas filas.
 *
 * <p>Desarmarlas en tablas costaría cuatro tablas, cuatro claves foráneas y
 * cuatro joins para reconstruir algo que se muestra junto, y a cambio congelaría
 * la forma del informe: cada motivo de rechazo nuevo —y van a aparecer, porque el
 * formato de los archivos de origen cambia— sería una migración. Lo que sí son
 * columnas de verdad es lo que se consulta o se cuenta: estado, período, métrica
 * y los dos contadores.
 *
 * <p>El precio, dicho claro: <b>estas listas no se pueden consultar por SQL</b>.
 * Es un precio que se paga a sabiendas porque hoy nada las consulta.
 *
 * <p>Un {@code ObjectMapper} propio y no el de Spring MVC: lo que se guarda en la
 * base no puede cambiar de forma porque alguien ajustó cómo se ve la API.
 */
final class ListasComoJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ListasComoJson() {
    }

    static String escribir(List<?> lista) {
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(lista);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el informe de la importación", e);
        }
    }

    static <T> List<T> leer(String json, TypeReference<List<T>> tipo) {
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
