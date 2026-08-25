package com.lautaro.multimetrico.persistencia.medicion;

import com.lautaro.multimetrico.medicion.MedicionRepository;
import com.lautaro.multimetrico.medicion.MedicionService;
import com.lautaro.multimetrico.medicion.domain.Medicion;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las mediciones en MySQL.
 *
 * <p>Solo inserta y lee, igual que el puerto: no hay {@code UPDATE} ni
 * {@code DELETE} porque una medición es un hecho fechado y corregir un número es
 * cargar otro, no pisar el anterior.
 *
 * <p>El orden en el que salen las lecturas lo explica {@link MedicionesJpa}, y no
 * es un detalle: es el desempate del valor vigente.
 */
@Repository
public class MedicionEnBase implements MedicionRepository {

    private final MedicionesJpa mediciones;

    public MedicionEnBase(MedicionesJpa mediciones) {
        this.mediciones = mediciones;
    }

    /**
     * Guarda el lote entero en una transacción.
     *
     * <p>El lote es un archivo semanal: o entra completo o no entra ninguna fila.
     * Media importación guardada es peor que ninguna, porque el informe diría que
     * entraron N y en la base habría menos, y nadie tendría cómo saber cuáles
     * faltan.
     *
     * <p>Devuelve el lote tal como llegó y no lo relee. Releerlo no aportaría
     * nada —los ids son internos y no salen al dominio— y sí traería de vuelta
     * los valores con la escala de la columna.
     */
    @Override
    @Transactional
    public List<Medicion> guardar(List<Medicion> lote) {
        mediciones.saveAll(lote.stream().map(MedicionEntidad::de).toList());
        return List.copyOf(lote);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicion> historia(YearMonth periodo) {
        return mediciones.findByPeriodoOrderByIdAsc(periodo).stream()
                .map(MedicionEntidad::aDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicion> historiaDe(YearMonth periodo, String legajo) {
        return mediciones
                .findByPeriodoAndLegajoClaveOrderByIdAsc(periodo, MedicionService.clave(legajo))
                .stream()
                .map(MedicionEntidad::aDominio)
                .toList();
    }
}
