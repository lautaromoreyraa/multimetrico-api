package com.lautaro.multimetrico.service.procesamiento.impl;

import com.lautaro.multimetrico.config.ConfiguracionDeImportacion;
import com.lautaro.multimetrico.domain.Importacion;
import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.importacion.Desajuste;
import com.lautaro.multimetrico.dto.importacion.Informe;
import com.lautaro.multimetrico.dto.planilla.FilaLeida;
import com.lautaro.multimetrico.exception.PlanillaRechazadaException;
import com.lautaro.multimetrico.exception.RecursoNoEncontradoException;
import com.lautaro.multimetrico.mapper.ImportacionMapper;
import com.lautaro.multimetrico.repository.ImportacionRepository;
import com.lautaro.multimetrico.service.planilla.LectorDePlanilla;
import com.lautaro.multimetrico.service.procesamiento.ProcesadorDeImportaciones;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.BiFunction;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * El procesamiento asincrónico de una importación.
 *
 * <p>Los dos fracasos que ataja son distintos y por eso se informan distinto:
 * {@link PlanillaRechazadaException} es culpa del archivo o del perfil, y
 * {@link RecursoNoEncontradoException} acá significa que el padrón del período no
 * está cargado, que no es culpa del archivo. Los dos terminan en
 * {@code RECHAZADA} —no entró nada— pero con desajustes que mandan a arreglar
 * cosas diferentes.
 */
@Service
public class ProcesadorDeImportacionesImpl implements ProcesadorDeImportaciones {

    private final LectorDePlanilla lector;
    private final ImportacionRepository importaciones;
    private final ImportacionMapper mapper;

    public ProcesadorDeImportacionesImpl(LectorDePlanilla lector, ImportacionRepository importaciones,
            ImportacionMapper mapper) {
        this.lector = lector;
        this.importaciones = importaciones;
        this.mapper = mapper;
    }

    @Override
    @Async(ConfiguracionDeImportacion.POOL)
    public void procesar(String id, PerfilDeImportacion perfil, YearMonth periodo, LocalDate corte,
            byte[] archivo, BiFunction<String, List<FilaLeida>, Informe> interprete) {

        // La fila EN_CURSO ya está commiteada: recibir() la guarda y recién
        // después llama acá, fuera de cualquier transacción abierta.
        Importacion importacion = importaciones.findById(id).orElseThrow();
        try {
            List<FilaLeida> filas = lector.leer(new ByteArrayInputStream(archivo), perfil);
            mapper.terminar(importacion, interprete.apply(id, filas));
        } catch (PlanillaRechazadaException e) {
            mapper.rechazar(importacion, e.desajustes());
        } catch (RecursoNoEncontradoException e) {
            // El padrón del período no está cargado. No es culpa del archivo, así
            // que se informa como desajuste del contexto y no como filas rechazadas.
            mapper.rechazar(importacion, List.of(Desajuste.padron(periodo, e.getMessage())));
        }
        importaciones.save(importacion);
    }
}
