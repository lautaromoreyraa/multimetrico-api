package com.lautaro.multimetrico.config;

import com.lautaro.multimetrico.service.seguridad.ServicioDeTokens;
import com.lautaro.multimetrico.service.seguridad.impl.ServicioDeTokensImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionDeTokens {

    @Bean
    public ServicioDeTokens servicioDeTokens(PropiedadesDeSeguridad propiedades) {
        PropiedadesDeSeguridad.Jwt jwt = propiedades.getJwt();
        return new ServicioDeTokensImpl(jwt.getSecreto(), jwt.getMinutosDeVigencia(), jwt.getEmisor());
    }
}
