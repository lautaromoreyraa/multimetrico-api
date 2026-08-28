package com.lautaro.multimetrico.mapper;

import com.lautaro.multimetrico.domain.Importacion;
import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.importacion.Desajuste;
import com.lautaro.multimetrico.dto.importacion.ImportacionResponse;
import com.lautaro.multimetrico.dto.importacion.Informe;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Traduce una importación entre el JSON, las entidades y las columnas de texto
 * donde vive el informe.
 *
 * <p>Es el único lugar que sabe que las cuatro listas del informe se guardan
 * serializadas. La entidad las tiene como {@link String} y no las entiende; los
 * servicios trabajan con {@link Informe} y no saben cómo se guarda. Ver
 * {@code JsonUtil} para el porqué de que no sean cuatro tablas.
 */
public interface ImportacionMapper {

    /** Una importación recién recibida, con el informe todavía vacío. */
    Importacion aEntidad(String id, PerfilDeImportacion perfil, YearMonth periodo, LocalDate corte);

    Informe informeDe(Importacion importacion);

    List<Desajuste> desajustesDe(Importacion importacion);

    /** Deja el informe anotado sin tocar el estado: para cuando se resuelve un pendiente. */
    void anotar(Importacion importacion, Informe informe);

    void terminar(Importacion importacion, Informe informe);

    void rechazar(Importacion importacion, List<Desajuste> desajustes);

    ImportacionResponse aResponse(Importacion importacion);
}
