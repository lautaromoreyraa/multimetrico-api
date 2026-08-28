package com.lautaro.multimetrico.mapper.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lautaro.multimetrico.domain.Importacion;
import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.importacion.Desajuste;
import com.lautaro.multimetrico.dto.importacion.DiscrepanciaDeLider;
import com.lautaro.multimetrico.dto.importacion.FilaPendiente;
import com.lautaro.multimetrico.dto.importacion.FilaRechazada;
import com.lautaro.multimetrico.dto.importacion.ImportacionResponse;
import com.lautaro.multimetrico.dto.importacion.Informe;
import com.lautaro.multimetrico.enumeration.EstadoDeImportacion;
import com.lautaro.multimetrico.mapper.ImportacionMapper;
import com.lautaro.multimetrico.util.JsonUtil;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * El mapper de importaciones, escrito a mano.
 *
 * <p>Concentra la serialización del informe. Que esté en un solo lugar es lo que
 * permite que la entidad no conozca los DTOs y que los servicios no sepan que
 * existe un JSON en una columna.
 */
@Component
public class ImportacionMapperImpl implements ImportacionMapper {

    private static final TypeReference<List<FilaRechazada>> RECHAZADAS = new TypeReference<>() {
    };
    private static final TypeReference<List<FilaPendiente>> PENDIENTES = new TypeReference<>() {
    };
    private static final TypeReference<List<DiscrepanciaDeLider>> DISCREPANCIAS = new TypeReference<>() {
    };
    private static final TypeReference<List<Desajuste>> DESAJUSTES = new TypeReference<>() {
    };

    @Override
    public Importacion aEntidad(String id, PerfilDeImportacion perfil, YearMonth periodo, LocalDate corte) {
        return new Importacion(id, perfil.nombre(), perfil.metrica(), periodo, corte);
    }

    @Override
    public Informe informeDe(Importacion importacion) {
        return new Informe(
                importacion.filasLeidas(),
                importacion.importadas(),
                JsonUtil.leer(importacion.rechazadasJson(), RECHAZADAS),
                JsonUtil.leer(importacion.pendientesJson(), PENDIENTES),
                JsonUtil.leer(importacion.discrepanciasJson(), DISCREPANCIAS));
    }

    @Override
    public List<Desajuste> desajustesDe(Importacion importacion) {
        return JsonUtil.leer(importacion.desajustesJson(), DESAJUSTES);
    }

    @Override
    public void anotar(Importacion importacion, Informe informe) {
        importacion.anotarInforme(
                informe.filasLeidas(),
                informe.importadas(),
                JsonUtil.escribir(informe.rechazadas()),
                JsonUtil.escribir(informe.pendientes()),
                JsonUtil.escribir(informe.discrepanciasDeLider()));
    }

    @Override
    public void terminar(Importacion importacion, Informe informe) {
        importacion.terminar(
                informe.filasLeidas(),
                informe.importadas(),
                JsonUtil.escribir(informe.rechazadas()),
                JsonUtil.escribir(informe.pendientes()),
                JsonUtil.escribir(informe.discrepanciasDeLider()));
    }

    @Override
    public void rechazar(Importacion importacion, List<Desajuste> desajustes) {
        importacion.rechazar(JsonUtil.escribir(desajustes));
    }

    @Override
    public ImportacionResponse aResponse(Importacion importacion) {
        Informe informe = informeDe(importacion);

        return new ImportacionResponse(
                importacion.id(),
                importacion.perfil(),
                importacion.metrica(),
                importacion.periodo(),
                importacion.fechaDeCorte(),
                importacion.estado(),
                informe.filasLeidas(),
                informe.importadas(),
                informe.rechazadas().size(),
                informe.pendientes().size(),
                cuadra(importacion, informe),
                informe.rechazadas().stream().map(ImportacionMapperImpl::rechazo).toList(),
                informe.pendientes().stream().map(ImportacionMapperImpl::pendiente).toList(),
                informe.discrepanciasDeLider().stream().map(ImportacionMapperImpl::discrepancia).toList(),
                desajustesDe(importacion).stream().map(Desajuste::toString).toList());
    }

    /**
     * ¿Entró todo? Para una importación RECHAZADA la respuesta es no.
     *
     * <p>{@link Informe#cuadra()} mira que las tres bolsas sumen las filas
     * leídas, y en una rechazada las cuatro cifras son cero: cero más cero
     * cierra, así que el campo que existe para contestar "¿entró todo?" decía
     * que sí cuando no entró <b>nada</b>. El invariante del informe sigue
     * valiendo —no se toca—; lo que se corrige es la lectura que la API
     * publica, que es la que alguien usa para decidir si el archivo se subió
     * bien.
     *
     * <p>RECHAZADA no es "terminó con rechazos": es que el archivo no coincidía
     * con el perfil y no se leyó ni una fila. El motivo está en
     * {@code desajustes}.
     */
    private static boolean cuadra(Importacion importacion, Informe informe) {
        return importacion.estado() != EstadoDeImportacion.RECHAZADA && informe.cuadra();
    }

    private static ImportacionResponse.RechazoResponse rechazo(FilaRechazada fila) {
        return new ImportacionResponse.RechazoResponse(
                fila.numeroDeFila(), fila.textoAsesor(), fila.textoValor(), fila.explicacion());
    }

    private static ImportacionResponse.PendienteResponse pendiente(FilaPendiente fila) {
        return new ImportacionResponse.PendienteResponse(
                fila.numeroDeFila(),
                fila.textoAsesor(),
                fila.valor() == null ? null : fila.valor().toPlainString(),
                fila.explicacion(),
                fila.candidatos());
    }

    private static ImportacionResponse.DiscrepanciaResponse discrepancia(DiscrepanciaDeLider d) {
        return new ImportacionResponse.DiscrepanciaResponse(
                d.numeroDeFila(), d.textoAsesor(), d.legajo(), d.liderDelArchivo(), d.liderDelPadron());
    }
}
