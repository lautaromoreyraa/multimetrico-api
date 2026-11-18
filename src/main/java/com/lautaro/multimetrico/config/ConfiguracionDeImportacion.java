package com.lautaro.multimetrico.config;

import java.util.concurrent.Executor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(PropiedadesDeImportacion.class)
public class ConfiguracionDeImportacion {

    public static final String POOL = "importaciones";

    private final PropiedadesDeImportacion propiedades;

    public ConfiguracionDeImportacion(PropiedadesDeImportacion propiedades) {
        this.propiedades = propiedades;
    }

    @Bean(POOL)
    public Executor importaciones() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(propiedades.hilos());
        pool.setMaxPoolSize(propiedades.hilos());
        pool.setQueueCapacity(propiedades.cola());
        pool.setThreadNamePrefix("importacion-");
        pool.initialize();
        return pool;
    }
}
