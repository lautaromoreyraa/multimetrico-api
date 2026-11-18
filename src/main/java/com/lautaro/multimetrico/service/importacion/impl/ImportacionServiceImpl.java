package com.lautaro.multimetrico.service.importacion.impl;

import com.lautaro.multimetrico.domain.Importacion;
import com.lautaro.multimetrico.domain.Medicion;
import com.lautaro.multimetrico.domain.NombreNormalizado;
import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.importacion.DiscrepanciaDeLider;
import com.lautaro.multimetrico.dto.importacion.FilaPendiente;
import com.lautaro.multimetrico.dto.importacion.FilaRechazada;
import com.lautaro.multimetrico.dto.importacion.Informe;
import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.dto.planilla.FilaLeida;
import com.lautaro.multimetrico.dto.resolucion.Coincidencia;
import com.lautaro.multimetrico.dto.resolucion.Pendiente;
import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import com.lautaro.multimetrico.enumeration.MotivoDeRechazo;
import com.lautaro.multimetrico.exception.RecursoNoEncontradoException;
import com.lautaro.multimetrico.mapper.ImportacionMapper;
import com.lautaro.multimetrico.repository.ImportacionRepository;
import com.lautaro.multimetrico.repository.PerfilRepository;
import com.lautaro.multimetrico.service.importacion.ImportacionService;
import com.lautaro.multimetrico.service.medicion.MedicionService;
import com.lautaro.multimetrico.service.padron.PadronService;
import com.lautaro.multimetrico.service.planilla.InterpreteDeNumeros;
import com.lautaro.multimetrico.service.planilla.LectorDePlanilla;
import com.lautaro.multimetrico.service.procesamiento.ProcesadorDeImportaciones;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportacionServiceImpl implements ImportacionService {

    private final LectorDePlanilla lector;
    private final ProcesadorDeImportaciones procesador;
    private final PerfilRepository perfiles;
    private final ImportacionRepository importaciones;
    private final PadronService padrones;
    private final MedicionService mediciones;
    private final ImportacionMapper mapper;

    public ImportacionServiceImpl(LectorDePlanilla lector, ProcesadorDeImportaciones procesador,
            PerfilRepository perfiles, ImportacionRepository importaciones, PadronService padrones,
            MedicionService mediciones, ImportacionMapper mapper) {

        this.lector = lector;
        this.procesador = procesador;
        this.perfiles = perfiles;
        this.importaciones = importaciones;
        this.padrones = padrones;
        this.mediciones = mediciones;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PerfilDeImportacion definirPerfil(PerfilDeImportacion perfil) {
        return perfiles.findByNombreClave(PerfilDeImportacion.clave(perfil.nombre()))
                .map(existente -> {
                    existente.actualizarCon(perfil);
                    return existente;
                })
                .orElseGet(() -> perfiles.save(perfil));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PerfilDeImportacion> perfiles() {
        return perfiles.findAllByOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilDeImportacion perfil(String nombre) {
        return perfiles.findByNombreClave(PerfilDeImportacion.clave(nombre))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay ningún perfil llamado '%s'".formatted(nombre)));
    }

    @Override
    @Transactional(readOnly = true)
    public Importacion buscar(String id) {
        return importaciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay ninguna importación con id %s".formatted(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Importacion> historial() {
        return importaciones.findAllByOrderByIniciadaDesc();
    }

    @Override
    public Importacion recibir(String nombrePerfil, YearMonth periodo, LocalDate corte, byte[] archivo) {
        PerfilDeImportacion perfil = perfil(nombrePerfil);
        Importacion recibida = importaciones.save(
                mapper.aEntidad(UUID.randomUUID().toString(), perfil, periodo, corte));

        procesador.procesar(recibida.id(), perfil, periodo, corte, archivo,
                (id, filas) -> interpretar(id, perfil, periodo, corte, filas));
        return recibida;
    }

    @Override
    public List<List<String>> inspeccionar(byte[] archivo, String hoja, int filaEncabezados) {
        return lector.muestra(new ByteArrayInputStream(archivo), hoja, filaEncabezados, 3);
    }

    @Override
    public List<String> hojasDe(byte[] archivo) {
        return lector.hojasDe(new ByteArrayInputStream(archivo));
    }

    @Override
    @Transactional
    public Importacion resolverPendiente(String id, int numeroDeFila, String legajo) {
        Importacion importacion = buscar(id);
        String legajoDelPadron = padrones.buscar(importacion.periodo()).porLegajo(legajo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El legajo %s no está en el padrón de %s".formatted(legajo, importacion.periodo())))
                .legajo();

        Informe informe = mapper.informeDe(importacion);
        FilaPendiente pendiente = informe.pendientes().stream()
                .filter(f -> f.numeroDeFila() == numeroDeFila)
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La importación %s no tiene ninguna fila %d pendiente".formatted(id, numeroDeFila)));

        mediciones.registrar(List.of(new Medicion(legajoDelPadron, importacion.metrica(),
                importacion.periodo(), pendiente.valor(), importacion.fechaDeCorte(), id)));

        mapper.anotar(importacion, informe.conPendientesResueltos(List.of(pendiente)));
        return importaciones.save(importacion);
    }

    private Informe interpretar(String id, PerfilDeImportacion perfil, YearMonth periodo,
            LocalDate corte, List<FilaLeida> filas) {

        List<FilaRechazada> rechazadas = new ArrayList<>();
        List<FilaLeida> conValor = new ArrayList<>();
        Map<Integer, BigDecimal> valores = new HashMap<>();

        for (FilaLeida fila : filas) {
            if (fila.textoAsesor() == null || fila.textoAsesor().isBlank()) {
                rechazadas.add(new FilaRechazada(fila.numeroDeFila(), fila.textoAsesor(),
                        fila.valor().texto(), MotivoDeRechazo.ASESOR_VACIO));
                continue;
            }
            Optional<BigDecimal> valor = InterpreteDeNumeros.interpretar(perfil.formatoNumero(), fila.valor());
            if (valor.isEmpty()) {
                rechazadas.add(new FilaRechazada(fila.numeroDeFila(), fila.textoAsesor(), fila.valor().texto(),
                        fila.valor().estaVacio()
                                ? MotivoDeRechazo.VALOR_VACIO
                                : MotivoDeRechazo.VALOR_NO_NUMERICO));
                continue;
            }
            conValor.add(fila);
            valores.put(fila.numeroDeFila(), valor.get());
        }

        Resolucion resolucion = padrones.resolver(periodo,
                conValor.stream().map(FilaLeida::textoAsesor).toList());

        Map<String, AsesorResponse> porTexto = new HashMap<>();
        for (Coincidencia coincidencia : resolucion.resueltos()) {
            porTexto.putIfAbsent(coincidencia.textoOriginal(), coincidencia.asesor());
        }
        Map<String, Pendiente> pendientesPorTexto = new HashMap<>();
        for (Pendiente pendiente : resolucion.pendientes()) {
            pendientesPorTexto.put(pendiente.textoOriginal(), pendiente);
        }

        List<Medicion> aRegistrar = new ArrayList<>();
        List<FilaPendiente> pendientes = new ArrayList<>();
        List<DiscrepanciaDeLider> discrepancias = new ArrayList<>();

        for (FilaLeida fila : conValor) {
            AsesorResponse asesor = porTexto.get(fila.textoAsesor());
            BigDecimal valor = valores.get(fila.numeroDeFila());

            if (asesor == null) {
                pendientes.add(FilaPendiente.de(fila.numeroDeFila(), valor,
                        pendientesPorTexto.get(fila.textoAsesor())));
                continue;
            }
            anotarDiscrepanciaDeLider(fila, asesor, discrepancias);
            aRegistrar.add(new Medicion(asesor.legajo(), perfil.metrica(), periodo, valor, corte, id));
        }

        if (!aRegistrar.isEmpty()) {
            mediciones.registrar(aRegistrar);
        }
        return new Informe(filas.size(), aRegistrar.size(), rechazadas, pendientes, discrepancias);
    }

    private void anotarDiscrepanciaDeLider(FilaLeida fila, AsesorResponse asesor,
            List<DiscrepanciaDeLider> discrepancias) {

        if (fila.textoLider() == null || fila.textoLider().isBlank()) {
            return;
        }
        NombreNormalizado delArchivo = NombreNormalizado.de(fila.textoLider());
        if (!delArchivo.equals(NombreNormalizado.de(asesor.lider()))) {
            discrepancias.add(new DiscrepanciaDeLider(fila.numeroDeFila(), fila.textoAsesor(),
                    asesor.legajo(), fila.textoLider(), asesor.lider()));
        }
    }
}
