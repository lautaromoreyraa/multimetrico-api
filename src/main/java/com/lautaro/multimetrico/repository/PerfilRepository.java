package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerfilRepository extends JpaRepository<PerfilDeImportacion, Long> {

    Optional<PerfilDeImportacion> findByNombreClave(String nombreClave);

    List<PerfilDeImportacion> findAllByOrderByNombreAsc();
}
