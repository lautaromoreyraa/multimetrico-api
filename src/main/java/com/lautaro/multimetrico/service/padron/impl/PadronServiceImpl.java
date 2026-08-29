package com.lautaro.multimetrico.service.padron.impl;

import com.lautaro.multimetrico.domain.AliasDeNombre;
import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import com.lautaro.multimetrico.exception.RecursoNoEncontradoException;
import com.lautaro.multimetrico.repository.AliasRepository;
import com.lautaro.multimetrico.repository.PadronRepository;
import com.lautaro.multimetrico.service.padron.PadronService;
import com.lautaro.multimetrico.service.resolucion.ResolutorDeNombres;
import com.lautaro.multimetrico.util.NombreNormalizado;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** El padrón por período, y el aprendizaje de alias que lo acompaña. */
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

    /**
     * Borra el padrón del período y escribe el nuevo entero.
     *
     * <p>El {@code flush} entre el borrado y el alta no es decorativo: sin él
     * Hibernate puede mandar el {@code INSERT} antes que el {@code DELETE} y la
     * unique del período lo rechaza.
     */
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

        // El save es lo que dispara el orphanRemoval: sin él, la lista queda
        // modificada en memoria y la fila sigue en la base.
        padrones.save(padron);
        return quitado;
    }

    @Override
    @Transactional(readOnly = true)
    public Resolucion resolver(YearMonth periodo, List<String> nombres) {
        return resolutor.resolver(buscar(periodo), nombres);
    }

    /**
     * Confirmar un nombre que ya estaba lo reapunta, no lo duplica.
     *
     * <p>Si alguien se equivocó al confirmar, volver a confirmar corrige. Un
     * insert a secas chocaría contra la unique del nombre y dejaría el error
     * escrito para siempre.
     */
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
