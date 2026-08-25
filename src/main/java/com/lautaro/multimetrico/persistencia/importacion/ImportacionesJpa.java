package com.lautaro.multimetrico.persistencia.importacion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ImportacionesJpa extends JpaRepository<ImportacionEntidad, String> {

    /** La más reciente primero: el historial se mira para ver qué se cargó recién. */
    List<ImportacionEntidad> findAllByOrderByIniciadaDesc();
}
