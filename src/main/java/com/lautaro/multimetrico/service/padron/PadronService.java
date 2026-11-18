package com.lautaro.multimetrico.service.padron;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.padron.AsesorRequest;
import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.dto.padron.CorreccionDeAsesorRequest;
import com.lautaro.multimetrico.dto.padron.PadronCargadoResponse;
import com.lautaro.multimetrico.dto.padron.PadronRequest;
import com.lautaro.multimetrico.dto.resolucion.AliasConfirmadoResponse;
import com.lautaro.multimetrico.dto.resolucion.AliasRequest;
import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import com.lautaro.multimetrico.dto.resolucion.ResolucionRequest;
import com.lautaro.multimetrico.dto.resolucion.ResolucionResponse;
import java.time.YearMonth;
import java.util.List;

public interface PadronService {

    Padron cargar(Padron padron);

    Padron buscar(YearMonth periodo);

    List<YearMonth> periodos();

    List<Asesor> equipo(YearMonth periodo, String lider);

    List<String> lideres(YearMonth periodo);

    Asesor agregarAsesor(YearMonth periodo, Asesor nuevo);

    Asesor corregirAsesor(YearMonth periodo, String legajo, String nombre, String lider);

    Asesor quitarAsesor(YearMonth periodo, String legajo);

    Resolucion resolver(YearMonth periodo, List<String> nombres);

    Asesor confirmarAlias(YearMonth periodo, String texto, String legajo);

    List<String> aliasDe(String legajo);

    PadronCargadoResponse cargar(PadronRequest request);

    List<AsesorResponse> asesores(YearMonth periodo, String lider);

    AsesorResponse agregarAsesor(YearMonth periodo, AsesorRequest request);

    AsesorResponse corregirAsesor(YearMonth periodo, String legajo, CorreccionDeAsesorRequest request);

    AsesorResponse quitarDelPadron(YearMonth periodo, String legajo);

    ResolucionResponse resolver(YearMonth periodo, ResolucionRequest request);

    AliasConfirmadoResponse confirmarAlias(YearMonth periodo, AliasRequest request);
}
