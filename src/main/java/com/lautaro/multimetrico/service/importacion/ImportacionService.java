package com.lautaro.multimetrico.service.importacion;

import com.lautaro.multimetrico.domain.Importacion;
import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.importacion.ImportacionResponse;
import com.lautaro.multimetrico.dto.perfil.InspeccionResponse;
import com.lautaro.multimetrico.dto.perfil.PerfilRequest;
import com.lautaro.multimetrico.dto.perfil.PerfilResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface ImportacionService {

    PerfilDeImportacion definirPerfil(PerfilDeImportacion perfil);

    List<PerfilDeImportacion> perfiles();

    PerfilDeImportacion perfil(String nombre);

    Importacion buscar(String id);

    List<Importacion> historial();

    Importacion recibir(String nombrePerfil, YearMonth periodo, LocalDate corte, byte[] archivo);

    List<List<String>> inspeccionar(byte[] archivo, String hoja, int filaEncabezados);

    List<String> hojasDe(byte[] archivo);

    Importacion resolverPendiente(String id, int numeroDeFila, String legajo);

    ImportacionResponse importar(String nombrePerfil, YearMonth periodo, LocalDate corte, byte[] archivo);

    ImportacionResponse ver(String id);

    List<ImportacionResponse> importaciones();

    ImportacionResponse asignarPendiente(String id, int numeroDeFila, String legajo);

    PerfilResponse definirPerfil(PerfilRequest request);

    List<PerfilResponse> perfilesDefinidos();

    InspeccionResponse inspeccionarPlanilla(byte[] archivo, String hoja, int filaEncabezados);
}
