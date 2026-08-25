package com.lautaro.multimetrico.persistencia.importacion;

import com.lautaro.multimetrico.importacion.PerfilRepository;
import com.lautaro.multimetrico.importacion.domain.PerfilDeImportacion;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PerfilEnBase implements PerfilRepository {

    private final PerfilesJpa perfiles;

    public PerfilEnBase(PerfilesJpa perfiles) {
        this.perfiles = perfiles;
    }

    /**
     * Guardar un perfil con un nombre que ya existe lo edita, no lo duplica.
     *
     * <p>Es la operación normal del módulo: la empresa cambia el reporte y
     * alguien corrige el nombre de una columna. Un alta a secas chocaría con la
     * unique del nombre y obligaría a borrar antes de corregir.
     */
    @Override
    @Transactional
    public PerfilDeImportacion guardar(PerfilDeImportacion perfil) {
        perfiles.findByNombreClave(PerfilEntidad.clave(perfil.nombre()))
                .ifPresentOrElse(
                        existente -> existente.copiarDe(perfil),
                        () -> perfiles.save(PerfilEntidad.de(perfil)));
        return perfil;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PerfilDeImportacion> porNombre(String nombre) {
        return perfiles.findByNombreClave(PerfilEntidad.clave(nombre)).map(PerfilEntidad::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PerfilDeImportacion> todos() {
        return perfiles.findAllByOrderByNombreAsc().stream().map(PerfilEntidad::aDominio).toList();
    }
}
