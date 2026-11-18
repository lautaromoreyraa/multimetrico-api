package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.domain.Importacion;
import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.importacion.Desajuste;
import com.lautaro.multimetrico.dto.importacion.ImportacionResponse;
import com.lautaro.multimetrico.dto.importacion.Informe;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface ImportacionMapper {

    Importacion aEntidad(String id, PerfilDeImportacion perfil, YearMonth periodo, LocalDate corte);

    Informe informeDe(Importacion importacion);

    List<Desajuste> desajustesDe(Importacion importacion);

    void anotar(Importacion importacion, Informe informe);

    void terminar(Importacion importacion, Informe informe);

    void rechazar(Importacion importacion, List<Desajuste> desajustes);

    ImportacionResponse aResponse(Importacion importacion);
}
