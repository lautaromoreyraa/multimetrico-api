package com.lautaro.multimetrico.repository;

import com.lautaro.multimetrico.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Los usuarios.
 *
 * <p>La búsqueda va siempre por {@code legajoClave} y nunca por {@code legajo}:
 * quien busca tiene que normalizar igual que quien guardó, y para eso está
 * {@link Usuario#clave(String)}. Buscar por la columna sin normalizar dejaría que
 * el resultado dependiera de la collation del motor, que no es la misma en MySQL
 * que en H2.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLegajoClave(String legajoClave);

    boolean existsByLegajoClave(String legajoClave);
}
