package com.lautaro.multimetrico.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enciende la auditoría de Spring Data JPA.
 *
 * <p>Hoy la usa una sola entidad: {@code Importacion.iniciada}, el instante en
 * que se recibió el archivo, anotado con {@code @CreatedDate}. Vale la pena igual
 * y no es ceremonia: ese campo es el que ordena el historial, y con auditoría no
 * existe un camino en el que una importación se guarde sin él.
 *
 * <p><b>Lo que deliberadamente no se audita son las mediciones.</b> Podría
 * parecer el candidato obvio —es la tabla que guarda historia—, pero una medición
 * ya viaja con su {@code fechaDeCorte}, que es el dato del negocio, y su orden de
 * carga es el {@code id}. Un {@code @CreatedDate} al lado sería una tercera fecha
 * que compite con las otras dos, y la pregunta "¿cuál de las tres mira el
 * multimétrico?" no tiene por qué existir.
 *
 * <p>Tampoco hay {@code @CreatedBy} ni {@code @LastModifiedBy}: no hay usuarios
 * todavía. Cuando entre seguridad, el {@code AuditorAware} va acá.
 */
@Configuration
@EnableJpaAuditing
public class ConfiguracionDeAuditoria {
}
