package com.lautaro.multimetrico.service.padron.impl;

import com.lautaro.multimetrico.domain.AliasDeNombre;
import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.NombreNormalizado;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.padron.AsesorRequest;
import com.lautaro.multimetrico.dto.padron.AsesorResponse;
import com.lautaro.multimetrico.dto.padron.CorreccionDeAsesorRequest;
import com.lautaro.multimetrico.dto.padron.PadronCargadoResponse;
import com.lautaro.multimetrico.dto.padron.PadronRequest;
import com.lautaro.multimetrico.dto.resolucion.AliasConfirmadoResponse;
import com.lautaro.multimetrico.dto.resolucion.AliasRequest;
import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import com.lautaro.multimetrico.dto.resolucion.ResolucionRequest;
import com.lautaro.multimetrico.dto.resolucion.ResolucionResponse;
import com.lautaro.multimetrico.exception.RecursoNoEncontradoException;
import com.lautaro.multimetrico.mapper.PadronMapper;
import com.lautaro.multimetrico.mapper.ResolucionMapper;
import com.lautaro.multimetrico.repository.AliasRepository;
import com.lautaro.multimetrico.repository.PadronRepository;
import com.lautaro.multimetrico.service.padron.PadronService;
import com.lautaro.multimetrico.service.resolucion.ResolutorDeNombres;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PadronServiceImpl implements PadronService {

    private final PadronRepository padrones;
    private final AliasRepository alias;
    private final ResolutorDeNombres resolutor;
    private final PadronMapper mapper;
    private final ResolucionMapper resolucionMapper;

    public PadronServiceImpl(PadronRepository padrones, AliasRepository alias, ResolutorDeNombres resolutor,
            PadronMapper mapper, ResolucionMapper resolucionMapper) {
        this.padrones = padrones;
        this.alias = alias;
        this.resolutor = resolutor;
        this.mapper = mapper;
        this.resolucionMapper = resolucionMapper;
    }

    @Override
    @Transactional
    public Padron cargar(Padron padron) {
        padrones.findByPeriodo(padron.periodo()).ifPresent(padrones::delete);
        padrones.flush();
        return padrones.save(padron);
    }

    @Override
    @Transactional(readOnly = true)
    public Padron buscar(YearMonth periodo) {
        return padrones.findByPeriodo(periodo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay padrón cargado para %s".formatted(periodo)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<YearMonth> periodos() {
        return padrones.findAllByOrderByPeriodoDesc().stream()
                .map(Padron::periodo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asesor> equipo(YearMonth periodo, String lider) {
        return buscar(periodo).equipoDe(lider);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> lideres(YearMonth periodo) {
        return buscar(periodo).lideres();
    }

    @Override
    @Transactional
    public Asesor agregarAsesor(YearMonth periodo, Asesor nuevo) {
        Padron padron = buscar(periodo);
        Asesor agregado = padron.agregar(nuevo);
        padrones.save(padron);
        return agregado;
    }

    @Override
    @Transactional
    public Asesor corregirAsesor(YearMonth periodo, String legajo, String nombre, String lider) {
        Padron padron = buscar(periodo);
        Asesor corregido = padron.corregir(legajo, nombre, lider);
        padrones.save(padron);
        return corregido;
    }

    @Override
    @Transactional
    public Asesor quitarAsesor(YearMonth periodo, String legajo) {
        Padron padron = buscar(periodo);
        Asesor quitado = padron.quitar(legajo);

        padrones.save(padron);
        return quitado;
    }

    @Override
    @Transactional(readOnly = true)
    public Resolucion resolver(YearMonth periodo, List<String> nombres) {
        return resolutor.resolver(buscar(periodo), nombres);
    }

    @Override
    @Transactional
    public Asesor confirmarAlias(YearMonth periodo, String texto, String legajo) {
        Asesor asesor = buscar(periodo).porLegajo(legajo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El legajo %s no está en el padrón de %s".formatted(legajo, periodo)));

        String nombre = NombreNormalizado.de(texto).valor();
        alias.findByNombre(nombre).ifPresentOrElse(
                existente -> existente.apuntarA(asesor.legajo()),
                () -> alias.save(new AliasDeNombre(nombre, asesor.legajo())));

        return asesor;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> aliasDe(String legajo) {
        return alias.findByLegajoOrderByNombreAsc(legajo).stream()
                .map(AliasDeNombre::nombre)
                .toList();
    }

    @Override
    @Transactional
    public PadronCargadoResponse cargar(PadronRequest request) {
        return mapper.aCargado(cargar(mapper.aEntidad(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsesorResponse> asesores(YearMonth periodo, String lider) {
        List<Asesor> asesores = lider == null ? buscar(periodo).asesores() : equipo(periodo, lider);
        return mapper.aResponses(asesores);
    }

    @Override
    @Transactional
    public AsesorResponse agregarAsesor(YearMonth periodo, AsesorRequest request) {
        return mapper.aResponse(agregarAsesor(periodo,
                new Asesor(request.legajo(), request.nombre(), request.lider())));
    }

    @Override
    @Transactional
    public AsesorResponse corregirAsesor(YearMonth periodo, String legajo, CorreccionDeAsesorRequest request) {
        return mapper.aResponse(corregirAsesor(periodo, legajo, request.nombre(), request.lider()));
    }

    @Override
    @Transactional
    public AsesorResponse quitarDelPadron(YearMonth periodo, String legajo) {
        return mapper.aResponse(quitarAsesor(periodo, legajo));
    }

    @Override
    @Transactional(readOnly = true)
    public ResolucionResponse resolver(YearMonth periodo, ResolucionRequest request) {
        return resolucionMapper.aResponse(resolver(periodo, request.nombres()));
    }

    @Override
    @Transactional
    public AliasConfirmadoResponse confirmarAlias(YearMonth periodo, AliasRequest request) {
        Asesor asesor = confirmarAlias(periodo, request.texto(), request.legajo());
        return new AliasConfirmadoResponse(
                request.texto(),
                asesor.legajo(),
                asesor.nombre(),
                aliasDe(asesor.legajo()));
    }
}
