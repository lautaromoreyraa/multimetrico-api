package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.dto.padron.Candidato;
import com.lautaro.multimetrico.dto.padron.PadronCargadoResponse;
import com.lautaro.multimetrico.dto.padron.PadronRequest;
import java.util.List;

public interface PadronMapper {

    Padron aEntidad(PadronRequest request);

    AsesorResponse aResponse(Asesor asesor);

    List<AsesorResponse> aResponses(List<Asesor> asesores);

    Candidato aCandidato(Asesor asesor);

    List<Candidato> aCandidatos(List<Asesor> asesores);

    PadronCargadoResponse aCargado(Padron padron);
}
