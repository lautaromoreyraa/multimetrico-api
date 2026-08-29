package com.lautaro.multimetrico.service.incentivo.impl;

import com.lautaro.multimetrico.domain.Asesor;
import com.lautaro.multimetrico.domain.Incentivo;
import com.lautaro.multimetrico.domain.Padron;
import com.lautaro.multimetrico.dto.incentivo.FilaSinCargar;
import com.lautaro.multimetrico.dto.incentivo.CargaDeIncentivos;
import com.lautaro.multimetrico.dto.incentivo.FilaDeIncentivo;
import com.lautaro.multimetrico.dto.incentivo.IncentivosResponse;
import com.lautaro.multimetrico.dto.perfil.PerfilDeIncentivos;
import com.lautaro.multimetrico.dto.planilla.FilaLeida;
import com.lautaro.multimetrico.dto.resolucion.Coincidencia;
import com.lautaro.multimetrico.dto.resolucion.Pendiente;
import com.lautaro.multimetrico.dto.resolucion.Resolucion;
import com.lautaro.multimetrico.exception.RecursoNoEncontradoException;
import com.lautaro.multimetrico.mapper.PadronMapper;
import com.lautaro.multimetrico.repository.IncentivoRepository;
import com.lautaro.multimetrico.service.incentivo.IncentivoService;
import com.lautaro.multimetrico.service.padron.PadronService;
import com.lautaro.multimetrico.service.planilla.LectorDePlanilla;
import com.lautaro.multimetrico.service.resolucion.ResolutorDeNombres;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga y lectura de los importes del mes.
 *
 * <p>Reusa las dos piezas difíciles que ya existen: {@link LectorDePlanilla} para
 * abrir el {@code .xlsx} y {@link ResolutorDeNombres} para pasar de un nombre
 * escrito por otro sistema a un legajo. Lo propio de acá es poco: decidir qué
 * hacer con cada fila y guardar el importe.
 */
@Service
public class IncentivoServiceImpl implements IncentivoService {

    private final IncentivoRepository repositorio;
    private final PadronService padrones;
    private final LectorDePlanilla lector;
    private final ResolutorDeNombres resolutor;
    private final PadronMapper padronMapper;

    public IncentivoServiceImpl(IncentivoRepository repositorio, PadronService padrones,
            LectorDePlanilla lector, ResolutorDeNombres resolutor, PadronMapper padronMapper) {
        this.repositorio = repositorio;
        this.padrones = padrones;
        this.lector = lector;
        this.resolutor = resolutor;
        this.padronMapper = padronMapper;
    }

    @Override
    @Transactional
    public CargaDeIncentivos cargar(YearMonth periodo, PerfilDeIncentivos perfil, byte[] archivo,
            String origen) {

        Padron padron = padrones.buscar(periodo);

        List<FilaLeida> leidas = lector
                .leer(new ByteArrayInputStream(archivo), perfil.aPerfilDeLectura())
                .stream()
                // Las filas totalmente vacías no son datos: son el final de la
                // hoja o una separación. Contarlas como rechazadas llenaría el
                // informe de ruido y escondería los rechazos que sí importan.
                .filter(fila -> !fila.vacia())
                .toList();

        // Una sola pasada por el resolutor con todos los nombres, en vez de una
        // por fila: resolver es lo más caro del proceso.
        Resolucion resolucion = resolutor.resolver(padron,
                leidas.stream().map(FilaLeida::textoAsesor).toList());

        Map<String, String> legajoPorTexto = resolucion.resueltos().stream()
                .collect(Collectors.toMap(
                        Coincidencia::textoOriginal,
                        c -> c.asesor().legajo(),
                        (uno, otro) -> uno));

        Map<String, Pendiente> ambiguos = resolucion.pendientes().stream()
                .collect(Collectors.toMap(Pendiente::textoOriginal, p -> p, (uno, otro) -> uno));

        List<Incentivo> aGuardar = new ArrayList<>();
        List<FilaSinCargar> rechazadas = new ArrayList<>();

        for (FilaLeida fila : leidas) {
            BigDecimal importe = fila.valor().numero();

            if (importe == null) {
                rechazadas.add(new FilaSinCargar(fila.numeroDeFila(), fila.textoAsesor(),
                        fila.valor().texto(),
                        "la columna del importe está vacía o no es un número"));
                continue;
            }

            String legajo = legajoPorTexto.get(fila.textoAsesor());
            if (legajo != null) {
                aGuardar.add(new Incentivo(periodo, legajo, importe, origen));
                continue;
            }

            rechazadas.add(new FilaSinCargar(fila.numeroDeFila(), fila.textoAsesor(),
                    fila.valor().texto(),
                    motivoDe(ambiguos.get(fila.textoAsesor()))));
        }

        repositorio.saveAll(aGuardar);

        return new CargaDeIncentivos(periodo, origen, leidas.size(), aGuardar.size(), rechazadas);
    }

    /**
     * Por qué no se pudo atribuir la fila.
     *
     * <p>El nombre ambiguo se rechaza <b>con los candidatos escritos en el
     * motivo</b>. No hay bolsa de pendientes como en la importación de métricas:
     * es más pobre, y es a propósito para esta primera versión. Lo que no se
     * resigna es que la fila desaparezca en silencio, que es el defecto que este
     * proyecto existe para no repetir.
     */
    private static String motivoDe(Pendiente pendiente) {
        if (pendiente == null) {
            return "no se parece a nadie del padrón de este período";
        }
        if (pendiente.candidatos().isEmpty()) {
            return "no se parece a nadie del padrón de este período";
        }
        return "el nombre coincide con más de un asesor: %s. Corregilo en el archivo y volvé a cargar"
                .formatted(pendiente.candidatos().stream()
                        .map(c -> "%s (%s)".formatted(c.nombre(), c.legajo()))
                        .collect(Collectors.joining(", ")));
    }

    @Override
    @Transactional(readOnly = true)
    public IncentivosResponse delEquipo(YearMonth periodo, String lider) {
        Padron padron = padrones.buscar(periodo);

        List<Asesor> equipo = lider == null || lider.isBlank()
                ? padron.asesores().stream().sorted(Comparator.comparing(Asesor::nombre)).toList()
                : padron.equipoDe(lider);

        if (equipo.isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "El líder '%s' no tiene asesores en el padrón de %s. Los líderes cargados son: %s"
                            .formatted(lider, periodo, String.join(", ", padron.lideres())));
        }

        Map<String, BigDecimal> vigentes = vigentesDe(periodo);

        List<FilaDeIncentivo> filas = equipo.stream()
                .map(asesor -> new FilaDeIncentivo(
                        padronMapper.aResponse(asesor),
                        vigentes.get(Incentivo.clave(asesor.legajo()))))
                .toList();

        BigDecimal total = filas.stream()
                .map(FilaDeIncentivo::importe)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new IncentivosResponse(
                periodo,
                lider,
                filas.size(),
                (int) filas.stream().filter(fila -> !fila.tieneImporte()).count(),
                total,
                filas);
    }

    /**
     * El importe vigente de cada legajo: <b>el de la última carga</b>.
     *
     * <p>La consulta viene ordenada por id, así que recorrer y pisar deja arriba
     * el más nuevo. Es el mismo contrato que el de las mediciones y depende de lo
     * mismo: si alguien cambia ese {@code ORDER BY}, la corrección que subieron
     * ayer deja de ganar sin que nada avise.
     */
    private Map<String, BigDecimal> vigentesDe(YearMonth periodo) {
        Map<String, BigDecimal> vigentes = new LinkedHashMap<>();
        for (Incentivo incentivo : repositorio.delPeriodo(periodo)) {
            vigentes.put(Incentivo.clave(incentivo.legajo()), incentivo.importe());
        }
        return vigentes;
    }
}
