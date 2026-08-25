package com.lautaro.multimetrico.importacion;

import com.lautaro.multimetrico.importacion.domain.Desajuste;
import com.lautaro.multimetrico.importacion.domain.DiscrepanciaDeLider;
import com.lautaro.multimetrico.importacion.domain.FilaLeida;
import com.lautaro.multimetrico.importacion.domain.FilaPendiente;
import com.lautaro.multimetrico.importacion.domain.FilaRechazada;
import com.lautaro.multimetrico.importacion.domain.Importacion;
import com.lautaro.multimetrico.importacion.domain.Informe;
import com.lautaro.multimetrico.importacion.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.importacion.domain.PlanillaRechazada;
import com.lautaro.multimetrico.medicion.MedicionService;
import com.lautaro.multimetrico.medicion.domain.Medicion;
import com.lautaro.multimetrico.padron.PadronService;
import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.padron.domain.Coincidencia;
import com.lautaro.multimetrico.padron.domain.NombreNormalizado;
import com.lautaro.multimetrico.padron.domain.Pendiente;
import com.lautaro.multimetrico.padron.domain.Resolucion;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Convierte una planilla semanal en mediciones.
 *
 * <h2>La propiedad que define el módulo</h2>
 *
 * <p><b>Ninguna fila se pierde.</b> Cada fila del archivo termina en exactamente
 * uno de tres lugares: importada, rechazada con su número de fila y su motivo, o
 * pendiente de resolución con los candidatos que el padrón sugiere. La suma de
 * los tres es la cantidad de filas leídas, y hay un test que lo verifica sobre
 * un archivo que mezcla todos los casos.
 *
 * <p>Esa es exactamente la diferencia con el proceso que reemplaza. Al pegar en
 * una planilla, la fila cuyo nombre no empalma no produce ningún síntoma: el
 * total sale igual, un poco más chico, y la persona se entera cuando reclama.
 *
 * <h2>Por qué no bloquea</h2>
 *
 * <p>La carga responde con el id de la importación apenas recibe el archivo y
 * procesa después. Un reporte con todos los equipos son decenas de miles de
 * filas, y cada una necesita resolverse contra el padrón; dejar a alguien
 * esperando frente a una pantalla en blanco —o peor, que el navegador corte por
 * timeout a mitad de camino— haría inservible la parte más valiosa, que es el
 * informe.
 */
@Service
public class ImportacionService {

    private final LectorDePlanilla lector;
    private final ProcesadorDeImportaciones procesador;
    private final PerfilRepository perfiles;
    private final ImportacionRepository importaciones;
    private final PadronService padrones;
    private final MedicionService mediciones;

    public ImportacionService(LectorDePlanilla lector, ProcesadorDeImportaciones procesador,
            PerfilRepository perfiles, ImportacionRepository importaciones,
            PadronService padrones, MedicionService mediciones) {
        this.lector = lector;
        this.procesador = procesador;
        this.perfiles = perfiles;
        this.importaciones = importaciones;
        this.padrones = padrones;
        this.mediciones = mediciones;
    }

    public PerfilDeImportacion definirPerfil(PerfilDeImportacion perfil) {
        return perfiles.guardar(perfil);
    }

    public List<PerfilDeImportacion> perfiles() {
        return perfiles.todos();
    }

    public PerfilDeImportacion perfil(String nombre) {
        return perfiles.porNombre(nombre).orElseThrow(() -> new NoSuchElementException(
                "No hay ningún perfil llamado '%s'".formatted(nombre)));
    }

    public Importacion buscar(String id) {
        return importaciones.porId(id).orElseThrow(() -> new NoSuchElementException(
                "No hay ninguna importación con id %s".formatted(id)));
    }

    public List<Importacion> historial() {
        return importaciones.todas();
    }

    /**
     * Recibe el archivo, deja anotada la importación y devuelve el id enseguida.
     *
     * <p>Los bytes se copian antes de salir del hilo de la petición: el
     * {@code InputStream} del upload se cierra cuando la respuesta se va, y el
     * procesamiento arranca después.
     */
    public Importacion recibir(String nombrePerfil, YearMonth periodo, LocalDate corte, byte[] archivo) {
        PerfilDeImportacion perfil = perfil(nombrePerfil);
        Importacion recibida = Importacion.enCurso(UUID.randomUUID().toString(), perfil, periodo, corte);
        importaciones.guardar(recibida);

        procesador.procesar(recibida.id(), perfil, periodo, corte, archivo,
                (id, filas) -> interpretar(id, perfil, periodo, corte, filas));
        return recibida;
    }

    /**
     * Reparte las filas en importadas, rechazadas y pendientes.
     *
     * <p>Los nombres se resuelven <b>todos juntos, en una sola pasada</b> contra
     * el padrón, y no fila por fila. No es una optimización: el resolutor necesita
     * ver el padrón entero para decidir si un nombre cortado apunta a una sola
     * persona o a varias, y esa decisión no se puede tomar mirando una fila
     * aislada.
     */
    private Informe interpretar(String id, PerfilDeImportacion perfil, YearMonth periodo,
            LocalDate corte, List<FilaLeida> filas) {

        List<FilaRechazada> rechazadas = new ArrayList<>();
        List<FilaLeida> conValor = new ArrayList<>();
        Map<Integer, BigDecimal> valores = new HashMap<>();

        for (FilaLeida fila : filas) {
            if (fila.textoAsesor() == null || fila.textoAsesor().isBlank()) {
                rechazadas.add(new FilaRechazada(fila.numeroDeFila(), fila.textoAsesor(),
                        fila.valor().texto(), FilaRechazada.Motivo.ASESOR_VACIO));
                continue;
            }
            Optional<BigDecimal> valor = perfil.formatoNumero().interpretar(fila.valor());
            if (valor.isEmpty()) {
                rechazadas.add(new FilaRechazada(fila.numeroDeFila(), fila.textoAsesor(), fila.valor().texto(),
                        fila.valor().estaVacio()
                                ? FilaRechazada.Motivo.VALOR_VACIO
                                : FilaRechazada.Motivo.VALOR_NO_NUMERICO));
                continue;
            }
            conValor.add(fila);
            valores.put(fila.numeroDeFila(), valor.get());
        }

        Resolucion resolucion = padrones.resolver(periodo,
                conValor.stream().map(FilaLeida::textoAsesor).toList());

        Map<String, Asesor> porTexto = resolucion.porTextoOriginal();
        Map<String, Pendiente> pendientesPorTexto = new HashMap<>();
        for (Pendiente pendiente : resolucion.pendientes()) {
            pendientesPorTexto.put(pendiente.textoOriginal(), pendiente);
        }

        List<Medicion> aRegistrar = new ArrayList<>();
        List<FilaPendiente> pendientes = new ArrayList<>();
        List<DiscrepanciaDeLider> discrepancias = new ArrayList<>();

        for (FilaLeida fila : conValor) {
            Asesor asesor = porTexto.get(fila.textoAsesor());
            BigDecimal valor = valores.get(fila.numeroDeFila());

            if (asesor == null) {
                Pendiente pendiente = pendientesPorTexto.get(fila.textoAsesor());
                pendientes.add(FilaPendiente.de(fila.numeroDeFila(), valor, pendiente));
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
    private void anotarDiscrepanciaDeLider(FilaLeida fila, Asesor asesor, List<DiscrepanciaDeLider> discrepancias) {
        if (fila.textoLider() == null || fila.textoLider().isBlank()) {
            return;
        }
        NombreNormalizado delArchivo = NombreNormalizado.de(fila.textoLider());
        if (!delArchivo.equals(asesor.liderNormalizado())) {
            discrepancias.add(new DiscrepanciaDeLider(fila.numeroDeFila(), fila.textoAsesor(),
                    asesor.legajo(), fila.textoLider(), asesor.lider()));
        }
    }

    /** Los encabezados del archivo con ejemplos, para poder armar un perfil. */
    public List<List<String>> inspeccionar(byte[] archivo, String hoja, int filaEncabezados) {
        return lector.muestra(new ByteArrayInputStream(archivo), hoja, filaEncabezados, 3);
    }

    public List<String> hojasDe(byte[] archivo) {
        return lector.hojasDe(new ByteArrayInputStream(archivo));
    }

    /**
     * Resuelve a mano una fila que había quedado pendiente.
     *
     * <p>Se resuelve <b>por importación</b> y no como alias global, y esa es la
     * diferencia que importa. Un nombre ambiguo como {@code SOSA ANA} es prefijo
     * de dos personas: confirmarlo como alias lo ataría para siempre a un legajo,
     * y en el archivo del mes que viene podría ser la otra. Acá la decisión vale
     * para esta fila de este archivo, que es todo lo que quien la toma puede
     * saber con certeza.
     */
    public Importacion resolverPendiente(String id, int numeroDeFila, String legajo) {
        Importacion importacion = buscar(id);
        Asesor asesor = padrones.buscar(importacion.periodo()).porLegajo(legajo)
                .orElseThrow(() -> new NoSuchElementException(
                        "El legajo %s no está en el padrón de %s".formatted(legajo, importacion.periodo())));

        FilaPendiente pendiente = importacion.informe().pendientes().stream()
                .filter(f -> f.numeroDeFila() == numeroDeFila)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "La importación %s no tiene ninguna fila %d pendiente".formatted(id, numeroDeFila)));

        mediciones.registrar(List.of(new Medicion(asesor.legajo(), importacion.metrica(),
                importacion.periodo(), pendiente.valor(), importacion.fechaDeCorte(), id)));

        return importaciones.guardar(new Importacion(
                importacion.id(), importacion.perfil(), importacion.metrica(), importacion.periodo(),
                importacion.fechaDeCorte(), importacion.estado(),
                importacion.informe().conPendientesResueltos(List.of(pendiente)),
                importacion.desajustes(), importacion.iniciada(), importacion.terminada()));
    }

    /** Las coincidencias de la última resolución, para poder auditarla. */
    public List<Coincidencia> comoSeResolvio(YearMonth periodo, List<String> nombres) {
        return padrones.resolver(periodo, nombres).resueltos();
    }
}
