package com.lautaro.multimetrico.multimetrico;

import com.lautaro.multimetrico.medicion.MedicionService;
import com.lautaro.multimetrico.medicion.domain.MedicionesVigentes;
import com.lautaro.multimetrico.multimetrico.domain.FilaDeMultimetrico;
import com.lautaro.multimetrico.multimetrico.domain.Multimetrico;
import com.lautaro.multimetrico.padron.PadronService;
import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.padron.domain.Padron;
import com.lautaro.multimetrico.pliego.PliegoService;
import com.lautaro.multimetrico.pliego.domain.Pliego;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

/**
 * Arma el multimétrico juntando las tres piezas que hoy están sueltas: quién
 * estaba en el equipo (padrón), con qué vara se lo mide (pliego) y qué números
 * tiene (mediciones).
 *
 * <p>Las tres se piden <b>del mismo período</b>. Es la regla que hace
 * reproducible el cálculo: el multimétrico de marzo usa el padrón de marzo, el
 * pliego de marzo y las mediciones de marzo, aunque se lo pida en septiembre.
 *
 * <p>El recorrido va desde el padrón hacia las mediciones y no al revés, y eso
 * decide qué se ve. Se recorre <b>el equipo</b>, y a cada asesor se le buscan sus
 * mediciones: el que no tiene ninguna aparece igual, con todas las métricas
 * faltantes. Al revés —recorrer las mediciones— el asesor sin datos
 * desaparecería de la lista, que es justo lo que pasa hoy cuando un nombre no
 * empalma: la fila no está y nadie la extraña.
 */
@Service
public class MultimetricoService {

    private final PliegoService pliegos;
    private final PadronService padrones;
    private final MedicionService mediciones;

    public MultimetricoService(PliegoService pliegos, PadronService padrones, MedicionService mediciones) {
        this.pliegos = pliegos;
        this.padrones = padrones;
        this.mediciones = mediciones;
    }

    /**
     * El multimétrico de un equipo. Con {@code lider} en {@code null}, la
     * operación entera.
     *
     * <p>Un líder sin nadie a cargo es un error y no una lista vacía. Devolver
     * 200 con cero filas se lee como "tu equipo no tiene datos cargados", que
     * manda a buscar el problema al lado equivocado: el problema no es que
     * falten mediciones, es que ese líder no existe en el padrón de ese mes
     * —renunció, cambió de nombre, o el período que se pidió no es el que se
     * quería—. Por eso el mensaje trae la lista de líderes que sí están: con eso
     * quien lo lee ve el suyo, o ve que no está y entiende por qué.
     *
     * <p>Las variantes de escritura no llegan hasta acá. {@code equipoDe}
     * compara nombres normalizados, así que "Peralta, Silvia" encuentra igual a
     * "PERALTA SILVIA".
     */
    public Multimetrico delEquipo(YearMonth periodo, String lider, String campania) {
        Pliego pliego = pliegos.buscar(periodo, campania);
        Padron padron = padrones.buscar(periodo);

        List<Asesor> equipo = lider == null || lider.isBlank()
                ? padron.asesores().stream().sorted(Comparator.comparing(Asesor::nombre)).toList()
                : padron.equipoDe(lider);

        if (equipo.isEmpty()) {
            throw new NoSuchElementException(
                    "El líder '%s' no tiene asesores en el padrón de %s. Los líderes cargados son: %s"
                            .formatted(lider, periodo, String.join(", ", padron.lideres())));
        }

        // Una sola pasada por la historia del mes en vez de una por asesor.
        Map<String, MedicionesVigentes> vigentes = mediciones.vigentesDelPeriodo(periodo);

        List<FilaDeMultimetrico> filas = equipo.stream()
                .map(asesor -> FilaDeMultimetrico.armar(pliego, asesor,
                        vigentes.getOrDefault(MedicionService.clave(asesor.legajo()), MedicionesVigentes.ninguna())))
                .toList();

        return new Multimetrico(periodo, pliego.campania(), lider, pliego.alcanceMaximo(), filas);
    }

    /**
     * La fila de una sola persona: la pantalla "mi multimétrico".
     *
     * <p>Va por el padrón igual que la del equipo. Un legajo con mediciones
     * cargadas pero fuera del padrón del período no devuelve una fila suelta:
     * devuelve 404, porque una fila sin asesor no se puede mostrar —no tiene
     * nombre ni líder— y, sobre todo, porque ese legajo fuera del padrón es un
     * problema de la carga que conviene que se vea.
     */
    public FilaDeMultimetrico deUnAsesor(YearMonth periodo, String legajo, String campania) {
        Pliego pliego = pliegos.buscar(periodo, campania);
        Asesor asesor = padrones.buscar(periodo).porLegajo(legajo)
                .orElseThrow(() -> new NoSuchElementException(
                        "El legajo %s no está en el padrón de %s".formatted(legajo, periodo)));

        return FilaDeMultimetrico.armar(pliego, asesor, mediciones.vigentesDe(periodo, legajo));
    }
}
