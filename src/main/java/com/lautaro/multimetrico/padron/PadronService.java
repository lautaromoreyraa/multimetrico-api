package com.lautaro.multimetrico.padron;

import com.lautaro.multimetrico.padron.domain.Asesor;
import com.lautaro.multimetrico.padron.domain.NombreNormalizado;
import com.lautaro.multimetrico.padron.domain.Padron;
import com.lautaro.multimetrico.padron.domain.Resolucion;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class PadronService {

    private final PadronRepository repositorio;
    private final ResolutorDeNombres resolutor;
    private final RegistroDeAlias alias;

    public PadronService(PadronRepository repositorio, ResolutorDeNombres resolutor, RegistroDeAlias alias) {
        this.repositorio = repositorio;
        this.resolutor = resolutor;
        this.alias = alias;
    }

    public Padron cargar(Padron padron) {
        return repositorio.guardar(padron);
    }

    public Padron buscar(YearMonth periodo) {
        return repositorio.buscar(periodo)
                .orElseThrow(() -> new NoSuchElementException(
                        "No hay padrón cargado para %s".formatted(periodo)));
    }

    public List<YearMonth> periodos() {
        return repositorio.periodos();
    }

    /** El equipo de un líder: el filtrado que hoy se hace a mano en cada archivo. */
    public List<Asesor> equipo(YearMonth periodo, String lider) {
        return buscar(periodo).equipoDe(lider);
    }

    public List<String> lideres(YearMonth periodo) {
        return buscar(periodo).lideres();
    }

    /**
     * Cruza una lista de nombres contra el padrón del período.
     *
     * <p>Es una operación de solo lectura, pensada para correr antes de importar:
     * deja ver cuántas filas van a empalmar y cuáles no, sin haber guardado nada.
     */
    public Resolucion resolver(YearMonth periodo, List<String> nombres) {
        return resolutor.resolver(buscar(periodo), nombres);
    }

    /**
     * Registra que un texto corresponde a un legajo.
     *
     * <p>Lo confirma una persona, y a partir de ahí el sistema no vuelve a
     * preguntar por ese texto. Se valida que el legajo exista en el padrón del
     * período: un alias que apunta a nadie es peor que no tenerlo.
     */
    public Asesor confirmarAlias(YearMonth periodo, String texto, String legajo) {
        Asesor asesor = buscar(periodo).porLegajo(legajo)
                .orElseThrow(() -> new NoSuchElementException(
                        "El legajo %s no está en el padrón de %s".formatted(legajo, periodo)));

        alias.confirmar(NombreNormalizado.de(texto), asesor.legajo());
        return asesor;
    }

    public List<String> aliasDe(String legajo) {
        return alias.aliasDe(legajo);
    }
}
