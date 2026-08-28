package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.perfil.InspeccionResponse;
import com.lautaro.multimetrico.dto.perfil.PerfilRequest;
import com.lautaro.multimetrico.dto.perfil.PerfilResponse;
import java.util.List;

/** Traduce el perfil de importación entre el JSON y la entidad. */
public interface PerfilMapper {

    /**
     * Arma el perfil desde el JSON.
     *
     * <p>Los dos campos del formato de número llegan planos y se juntan acá en el
     * objeto embebido. Cuando no vienen se usan los valores de una planilla en
     * español —coma decimal, número tal cual—, que es el caso mayoritario.
     */
    PerfilDeImportacion aEntidad(PerfilRequest request);

    PerfilResponse aResponse(PerfilDeImportacion perfil);

    List<PerfilResponse> aResponses(List<PerfilDeImportacion> perfiles);

    /**
     * @param columnas una lista por columna, con el encabezado primero y los
     *     ejemplos después, tal como la devuelve el lector
     */
    InspeccionResponse aInspeccion(List<String> hojas, int filaEncabezados, List<List<String>> columnas);
}
