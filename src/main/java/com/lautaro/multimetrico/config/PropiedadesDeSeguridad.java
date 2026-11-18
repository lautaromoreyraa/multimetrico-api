package com.lautaro.multimetrico.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "multimetrico.seguridad")
public class PropiedadesDeSeguridad {

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
