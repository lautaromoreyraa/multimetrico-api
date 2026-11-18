package com.lautaro.multimetrico.service.padron.impl;

import com.lautaro.multimetrico.domain.AliasDeNombre;
import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.NombreNormalizado;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import com.lautaro.multimetrico.exception.RecursoNoEncontradoException;
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

    public PadronServiceImpl(PadronRepository padrones, AliasRepository alias, ResolutorDeNombres resolutor) {
        this.padrones = padrones;
        this.alias = alias;
        this.resolutor = resolutor;
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
}
