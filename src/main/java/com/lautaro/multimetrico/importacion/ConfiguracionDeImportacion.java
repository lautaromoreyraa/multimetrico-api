package com.lautaro.multimetrico.importacion;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * El pool que procesa las importaciones.
 *
 * <p>Es propio y chico a propósito, no el executor por defecto de Spring. Leer una
 * planilla entera con POI la carga en memoria; dejar que se apilen diez importaciones
 * grandes en paralelo es la forma más rápida de quedarse sin heap. Con dos hilos
 * las importaciones hacen cola, que para una tarea semanal es exactamente lo que
 * se quiere.
 *
 * <p>La cola tiene tope. Sin tope, una avalancha de cargas se acumula en silencio
 * hasta reventar; con tope, la que sobra falla de entrada y se ve.
 */
@Configuration
@EnableAsync
public class ConfiguracionDeImportacion {

    @Bean("importaciones")
    public Executor importaciones() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(2);
        pool.setMaxPoolSize(2);
        pool.setQueueCapacity(50);
        pool.setThreadNamePrefix("importacion-");
        pool.initialize();
        return pool;
    }
}
