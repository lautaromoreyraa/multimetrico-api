package com.lautaro.multimetrico.config;

import com.lautaro.multimetrico.domain.Usuario;
import com.lautaro.multimetrico.enumeration.Rol;
import com.lautaro.multimetrico.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CreadorDeAdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CreadorDeAdminInicial.class);

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final PropiedadesDeSeguridad propiedades;

    public CreadorDeAdminInicial(UsuarioRepository repositorio, PasswordEncoder encoder,
            PropiedadesDeSeguridad propiedades) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.propiedades = propiedades;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repositorio.count() > 0) {
            return;
        }

        String legajo = propiedades.getAdminInicial().getLegajo();
        String password = propiedades.getAdminInicial().getPassword();

        if (password == null || password.isBlank()) {
            log.warn("""

                    ┌───────────────────────────────────────────────────────────────┐
                    │ No hay ningún usuario y no se creó el administrador inicial.   │
                    │                                                               │
                    │ Nadie puede entrar todavía. Para crearlo, definí               │
                    │ ADMIN_PASSWORD en el entorno y reiniciá.                       │
                    │                                                               │
                    │ No hay una contraseña por defecto a propósito: dejaría una     │
                    │ cuenta de administrador que adivina cualquiera.                │
                    └───────────────────────────────────────────────────────────────┘""");
            return;
        }

        repositorio.save(new Usuario(legajo, "Administrador", encoder.encode(password), Rol.ADMIN, null));
        log.info("Se creó el administrador inicial con el legajo '{}'. La contraseña salió del entorno.", legajo);
    }
}
