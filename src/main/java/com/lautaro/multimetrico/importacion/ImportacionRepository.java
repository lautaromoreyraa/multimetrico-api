package com.lautaro.multimetrico.importacion;

import com.lautaro.multimetrico.importacion.domain.Importacion;
import java.util.List;
import java.util.Optional;

public interface ImportacionRepository {

    Importacion guardar(Importacion importacion);

    Optional<Importacion> porId(String id);

    List<Importacion> todas();
}
