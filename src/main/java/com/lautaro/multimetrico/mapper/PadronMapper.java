package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.dto.padron.Candidato;
import com.lautaro.multimetrico.dto.padron.PadronCargadoResponse;
import com.lautaro.multimetrico.dto.padron.PadronRequest;
import java.util.List;

/**
 * Traduce el padrón entre el JSON y las entidades.
 *
 * <p>Un asesor sale en dos formas según para qué se lo mire: completo
 * ({@link AsesorResponse}, con su líder) cuando se lista el padrón o se informa a
 * quién se resolvió una fila, y reducido ({@link Candidato}, solo legajo y
 * nombre) cuando se lo ofrece para que una persona desempate. La segunda no lleva
 * el líder a propósito: quien está eligiendo entre dos nombres parecidos no
 * necesita ese dato, y mostrarlo invita a usarlo como criterio de desempate
 * cuando el archivo que lo trae puede tener la nómina vieja.
 */
public interface PadronMapper {

    Padron aEntidad(PadronRequest request);

    AsesorResponse aResponse(Asesor asesor);

    List<AsesorResponse> aResponses(List<Asesor> asesores);

    Candidato aCandidato(Asesor asesor);

    List<Candidato> aCandidatos(List<Asesor> asesores);

    PadronCargadoResponse aCargado(Padron padron);
}
