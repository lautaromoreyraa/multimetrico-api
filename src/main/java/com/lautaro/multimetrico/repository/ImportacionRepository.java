package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.Importacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportacionRepository extends JpaRepository<Importacion, String> {

    List<Importacion> findAllByOrderByIniciadaDesc();
}
