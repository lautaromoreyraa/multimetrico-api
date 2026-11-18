package com.lautaro.multimetrico.service.importacion;

import com.lautaro.multimetrico.domain.Importacion;
import com.lautaro.multimetrico.domain.PerfilDeImportacion;
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
}
