package com.lautaro.multimetrico.config;

import java.util.concurrent.Executor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * El pool que procesa las importaciones.
 *
 * <p>Es propio y chico a propósito, no el executor por defecto de Spring. Leer una
 * planilla entera con POI la carga en memoria; dejar que se apilen diez
 * importaciones grandes en paralelo es la forma más rápida de quedarse sin heap.
 * Con dos hilos las importaciones hacen cola, que para una tarea semanal es
 * exactamente lo que se quiere.
 *
 * <p>Los números salen de {@link PropiedadesDeImportacion} y no están escritos
 * acá: el día que un archivo tarde de más, subirlos es editar el
 * {@code application.yml}, no tocar código.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(PropiedadesDeImportacion.class)
public class ConfiguracionDeImportacion {

    /** El nombre del bean es el que nombra {@code @Async("importaciones")}. */
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
