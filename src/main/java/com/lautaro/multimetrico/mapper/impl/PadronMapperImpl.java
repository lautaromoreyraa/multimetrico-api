package com.lautaro.multimetrico.mapper.impl;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.dto.padron.Candidato;
import com.lautaro.multimetrico.dto.padron.PadronCargadoResponse;
import com.lautaro.multimetrico.dto.padron.PadronRequest;
import com.lautaro.multimetrico.mapper.PadronMapper;
import com.lautaro.multimetrico.util.PeriodoUtil;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PadronMapperImpl implements PadronMapper {

    @Override
    public Padron aEntidad(PadronRequest request) {
        List<Asesor> asesores = request.asesores().stream()
                .map(a -> new Asesor(a.legajo(), a.nombre(), a.lider()))
                .toList();

        return new Padron(request.periodo(), asesores);
    }

    @Override
    public AsesorResponse aResponse(Asesor asesor) {
        return new AsesorResponse(asesor.legajo(), asesor.nombre(), asesor.lider());
    }

    @Override
    public List<AsesorResponse> aResponses(List<Asesor> asesores) {
        return asesores.stream().map(this::aResponse).toList();
    }

    @Override
    public Candidato aCandidato(Asesor asesor) {
        return new Candidato(asesor.legajo(), asesor.nombre());
    }

    @Override
    public List<Candidato> aCandidatos(List<Asesor> asesores) {
        return asesores.stream().map(this::aCandidato).toList();
    }

    @Override
    public PadronCargadoResponse aCargado(Padron padron) {
        return new PadronCargadoResponse(
                PeriodoUtil.aTexto(padron.periodo()),
                padron.tamano(),
                padron.lideres());
    }
}
