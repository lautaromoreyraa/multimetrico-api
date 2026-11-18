package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.perfil.InspeccionResponse;
import com.lautaro.multimetrico.dto.perfil.PerfilRequest;
import com.lautaro.multimetrico.dto.perfil.PerfilResponse;
import java.util.List;

public interface PerfilMapper {

    PerfilDeImportacion aEntidad(PerfilRequest request);

    PerfilResponse aResponse(PerfilDeImportacion perfil);

    List<PerfilResponse> aResponses(List<PerfilDeImportacion> perfiles);

    InspeccionResponse aInspeccion(List<String> hojas, int filaEncabezados, List<List<String>> columnas);
}
