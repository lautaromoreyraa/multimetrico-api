package com.lautaro.multimetrico.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Lo configurable de la seguridad, todo desde el entorno.
 *
 * <h2>El secreto no tiene default, y es lo importante de esta clase</h2>
 *
 * <p>Un default hace que la aplicación arranque igual con una clave conocida.
 * Entonces el día que alguien se olvida de configurarla en producción no se
 * entera de nada: los tokens se firman con la clave que está en el repositorio y
 * cualquiera que lo haya leído puede fabricarse uno de administrador.
 *
 * <p>Por eso acá falla al arrancar. Un arranque que falla con un mensaje claro se
 * arregla en dos minutos; una aplicación andando con la clave de ejemplo puede
 * pasar meses sin que nadie lo note.
 *
 * <p>El mínimo de 32 caracteres tampoco es capricho: HS256 firma con una clave de
 * 256 bits, y JJWT rechaza una más corta con un {@code WeakKeyException} que no
 * explica qué hay que hacer. Mejor decirlo acá.
 */
@Component
@ConfigurationProperties(prefix = "multimetrico.seguridad")
public class PropiedadesDeSeguridad {

    /** Caracteres mínimos del secreto: 32 × 8 = los 256 bits que pide HS256. */
    private static final int MINIMO_DEL_SECRETO = 32;

    private Jwt jwt = new Jwt();
    private AdminInicial adminInicial = new AdminInicial();

    @PostConstruct
    void validar() {
        if (jwt.secreto == null || jwt.secreto.isBlank()) {
            throw new IllegalStateException("""
                    Falta el secreto de firma de los tokens.

                    Definí JWT_SECRETO en el entorno, o
                    multimetrico.seguridad.jwt.secreto en application-local.properties.

                    No hay un valor por defecto a propósito: un secreto con default
                    es un secreto que termina siendo el mismo en todas las
                    instalaciones. Para generarlo: openssl rand -base64 48""");
        }

        if (jwt.secreto.length() < MINIMO_DEL_SECRETO) {
            throw new IllegalStateException(
                    ("El secreto de firma tiene %d caracteres y hacen falta al menos %d. "
                            + "HS256 firma con una clave de 256 bits y una más corta la rechaza la "
                            + "librería. Para generarlo: openssl rand -base64 48")
                            .formatted(jwt.secreto.length(), MINIMO_DEL_SECRETO));
        }

        if (jwt.minutosDeVigencia <= 0) {
            throw new IllegalStateException(
                    "La vigencia del token tiene que ser mayor que cero, y es %d minutos."
                            .formatted(jwt.minutosDeVigencia));
        }
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public AdminInicial getAdminInicial() {
        return adminInicial;
    }

    public void setAdminInicial(AdminInicial adminInicial) {
        this.adminInicial = adminInicial;
    }

    public static class Jwt {

        private String secreto;

        /** Ocho horas: un turno. Ver `application.properties`. */
        private long minutosDeVigencia = 480;

        private String emisor = "multimetrico-api";

        public String getSecreto() {
            return secreto;
        }

        public void setSecreto(String secreto) {
            this.secreto = secreto;
        }

        public long getMinutosDeVigencia() {
            return minutosDeVigencia;
        }

        public void setMinutosDeVigencia(long minutosDeVigencia) {
            this.minutosDeVigencia = minutosDeVigencia;
        }

        public String getEmisor() {
            return emisor;
        }

        public void setEmisor(String emisor) {
            this.emisor = emisor;
        }
    }

    /**
     * El primer administrador, para poder entrar a una base recién creada.
     *
     * <p>Sin contraseña configurada no se crea nada. Es deliberado: un admin con
     * contraseña por defecto en una base que acaba de quedar expuesta es peor que
     * una base a la que todavía no se puede entrar.
     */
    public static class AdminInicial {

        private String legajo = "admin";
        private String password;

        public String getLegajo() {
            return legajo;
        }

        public void setLegajo(String legajo) {
            this.legajo = legajo;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
