package com.lautaro.multimetrico.service.padron;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.resolucion.Resolucion;
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
}
