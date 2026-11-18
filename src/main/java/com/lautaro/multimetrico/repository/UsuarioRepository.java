package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLegajoClave(String legajoClave);

    boolean existsByLegajoClave(String legajoClave);
}
