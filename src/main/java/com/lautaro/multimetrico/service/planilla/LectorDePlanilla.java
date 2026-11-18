package com.lautaro.multimetrico.service.planilla;

import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.planilla.FilaLeida;
import java.io.InputStream;
import java.util.List;

public interface LectorDePlanilla {

    List<FilaLeida> leer(InputStream entrada, PerfilDeImportacion perfil);

    List<List<String>> muestra(InputStream entrada, String hoja, int filaEncabezados, int ejemplos);

    List<String> hojasDe(InputStream entrada);
}
