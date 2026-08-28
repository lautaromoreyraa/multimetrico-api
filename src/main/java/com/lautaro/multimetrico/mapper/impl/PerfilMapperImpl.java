package com.lautaro.multimetrico.mapper.impl;

import com.lautaro.multimetrico.domain.FormatoDeNumero;
import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.perfil.InspeccionResponse;
import com.lautaro.multimetrico.dto.perfil.PerfilRequest;
import com.lautaro.multimetrico.dto.perfil.PerfilResponse;
import com.lautaro.multimetrico.enumeration.EscalaDeNumero;
import com.lautaro.multimetrico.enumeration.SeparadorDecimal;
import com.lautaro.multimetrico.mapper.PerfilMapper;
import java.util.List;
import org.springframework.stereotype.Component;

/** El mapper del perfil de importación, escrito a mano. */
@Component
public class PerfilMapperImpl implements PerfilMapper {

    @Override
    public PerfilDeImportacion aEntidad(PerfilRequest request) {
        FormatoDeNumero formato = new FormatoDeNumero(
                request.separadorDecimal() == null ? SeparadorDecimal.COMA : request.separadorDecimal(),
                request.escala() == null ? EscalaDeNumero.TAL_CUAL : request.escala());

        return new PerfilDeImportacion(
                request.nombre(),
                request.metrica(),
                request.hoja(),
                request.filaEncabezados(),
                request.columnaAsesor(),
                request.columnaValor(),
                request.columnaLider(),
                formato);
    }

    @Override
    public PerfilResponse aResponse(PerfilDeImportacion perfil) {
        return new PerfilResponse(
                perfil.nombre(),
                perfil.metrica(),
                perfil.hoja(),
                perfil.filaEncabezados(),
                perfil.columnaAsesor(),
                perfil.columnaValor(),
                perfil.columnaLider(),
                new PerfilResponse.FormatoDeNumeroResponse(
                        perfil.formatoNumero().separadorDecimal(),
                        perfil.formatoNumero().escala()));
    }

    @Override
    public List<PerfilResponse> aResponses(List<PerfilDeImportacion> perfiles) {
        return perfiles.stream().map(this::aResponse).toList();
    }

    @Override
    public InspeccionResponse aInspeccion(List<String> hojas, int filaEncabezados, List<List<String>> columnas) {
        List<InspeccionResponse.ColumnaResponse> descripcion = columnas.stream()
                .map(columna -> new InspeccionResponse.ColumnaResponse(
                        columna.getFirst(),
                        columna.subList(1, columna.size())))
                .toList();

        return new InspeccionResponse(hojas, filaEncabezados, descripcion);
    }
}
