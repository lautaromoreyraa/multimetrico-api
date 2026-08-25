package com.lautaro.multimetrico.persistencia.padron;

import com.lautaro.multimetrico.padron.RegistroDeAlias;
import com.lautaro.multimetrico.padron.domain.NombreNormalizado;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AliasEnBase implements RegistroDeAlias {

    private final AliasJpa alias;

    public AliasEnBase(AliasJpa alias) {
        this.alias = alias;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> legajoDe(NombreNormalizado nombre) {
        return alias.findByNombre(nombre.valor()).map(AliasEntidad::legajo);
    }

    /**
     * Confirmar un nombre que ya estaba lo reapunta, no lo duplica.
     *
     * <p>Es el mismo {@code Map.put} de la versión en memoria: si alguien se
     * equivocó al confirmar, volver a confirmar corrige. Un insert a secas
     * chocaría contra la unique del nombre y dejaría el error escrito para
     * siempre.
     */
    @Override
    @Transactional
    public void confirmar(NombreNormalizado nombre, String legajo) {
        alias.findByNombre(nombre.valor())
                .ifPresentOrElse(
                        existente -> existente.apuntarA(legajo),
                        () -> alias.save(new AliasEntidad(nombre.valor(), legajo)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> aliasDe(String legajo) {
        return alias.findByLegajoOrderByNombreAsc(legajo).stream()
                .map(AliasEntidad::nombre)
                .toList();
    }
}
