package com.lautaro.multimetrico.service.importacion.impl;

import com.lautaro.multimetrico.domain.Importacion;
import com.lautaro.multimetrico.domain.Medicion;
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
import com.lautaro.multimetrico.service.planilla.LectorDePlanilla;
import com.lautaro.multimetrico.service.procesamiento.ProcesadorDeImportaciones;
import com.lautaro.multimetrico.util.NombreNormalizado;
import com.lautaro.multimetrico.util.NumeroUtil;
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

/** La importación de planillas. La propiedad que define el módulo está en la interfaz. */
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

    /**
     * Recibe el archivo, deja anotada la importación y devuelve el id enseguida.
     *
     * <p><b>Este método no lleva {@code @Transactional} y es a propósito.</b> El
     * procesamiento arranca en otro hilo y lo primero que hace es leer la fila por
     * su id: si el alta estuviera dentro de una transacción todavía abierta, ese
     * hilo no la encontraría. Cada {@code save} abre la suya y commitea, y recién
     * después se llama al procesador.
     */
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

    /**
     * Reparte las filas en importadas, rechazadas y pendientes.
     *
     * <p>Los nombres se resuelven <b>todos juntos, en una sola pasada</b> contra
     * el padrón, y no fila por fila. No es una optimización: el resolutor necesita
     * ver el padrón entero para decidir si un nombre cortado apunta a una sola
     * persona o a varias, y esa decisión no se puede tomar mirando una fila
     * aislada.
     *
     * <p>Corre en el hilo del pool de importaciones, invocada como función desde
     * {@code ProcesadorDeImportaciones}.
     */
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
            Optional<BigDecimal> valor = NumeroUtil.interpretar(perfil.formatoNumero(), fila.valor());
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

        // Que no se haya resuelto ni una fila es un resultado posible —un archivo
        // entero de gente que no está en el padrón—, no un error. El servicio de
        // mediciones rechaza los lotes vacíos, así que acá se evita llamarlo.
        if (!aRegistrar.isEmpty()) {
            mediciones.registrar(aRegistrar);
        }
        return new Informe(filas.size(), aRegistrar.size(), rechazadas, pendientes, discrepancias);
    }

    /**
     * Si el archivo dice que el asesor es de otro equipo que el padrón, se anota
     * pero <b>no se rechaza la fila</b>.
     *
     * <p>Manda el padrón: es la fuente de verdad de quién está bajo qué líder, y
     * la medición pertenece a la persona igual. Pero la discrepancia se informa,
     * porque casi siempre significa que alguien cambió de equipo y el padrón del
     * período quedó viejo — y eso conviene saberlo antes de que el multimétrico
     * muestre a la persona en la tabla equivocada.
     */
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
