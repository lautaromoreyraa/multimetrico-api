package com.lautaro.multimetrico.persistencia.importacion;

import com.lautaro.multimetrico.importacion.ImportacionRepository;
import com.lautaro.multimetrico.importacion.domain.Importacion;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las importaciones en MySQL.
 *
 * <p>Una importación se guarda varias veces sobre la misma fila: primero
 * {@code EN_CURSO} apenas llega el archivo, después {@code TERMINADA} o
 * {@code RECHAZADA} cuando el procesamiento la resuelve, y otra vez cada vez que
 * alguien resuelve un pendiente a mano. Por eso {@link #guardar} actualiza si ya
 * existe en vez de insertar.
 *
 * <p>La segunda y la tercera escritura pasan por un hilo del pool de
 * importaciones, no por el de la petición. Cada método acá abre su propia
 * transacción, así que eso no es un problema; lo que sí importa es que la fila
 * {@code EN_CURSO} esté <b>commiteada</b> antes de que arranque el hilo
 * asincrónico, que es el caso porque {@code recibir} guarda y recién después
 * llama al procesador.
 */
@Repository
public class ImportacionEnBase implements ImportacionRepository {

    private final ImportacionesJpa importaciones;

    public ImportacionEnBase(ImportacionesJpa importaciones) {
        this.importaciones = importaciones;
    }

    @Override
    @Transactional
    public Importacion guardar(Importacion importacion) {
        importaciones.findById(importacion.id())
                .ifPresentOrElse(
                        existente -> existente.copiarDe(importacion),
                        () -> importaciones.save(ImportacionEntidad.de(importacion)));
        return importacion;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Importacion> porId(String id) {
        return importaciones.findById(id).map(ImportacionEntidad::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Importacion> todas() {
        return importaciones.findAllByOrderByIniciadaDesc().stream()
                .map(ImportacionEntidad::aDominio)
                .toList();
    }
}
