package com.lautaro.multimetrico.importacion.domain;

import com.lautaro.multimetrico.pliego.domain.Metrica;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Una corrida de importación: qué archivo entró, con qué perfil, y cómo le fue.
 *
 * <p>Queda guardada aunque haya salido mal. Es lo que permite volver sobre "el
 * TMO de la semana pasada quedó raro" y ver que ese día se rechazaron cuarenta
 * filas — hoy esa información no existe en ninguna parte, porque pegar en una
 * planilla no deja registro.
 *
 * <p>El estado {@link Estado#EN_CURSO} existe porque la carga no bloquea: el que
 * sube el archivo recibe el id enseguida y consulta después. Un archivo de
 * veinte mil filas no puede tener a alguien esperando frente a una pantalla en
 * blanco.
 */
public record Importacion(
        String id,
        String perfil,
        Metrica metrica,
        YearMonth periodo,
        LocalDate fechaDeCorte,
        Estado estado,
        Informe informe,
        List<Desajuste> desajustes,
        Instant iniciada,
        Instant terminada) {

    public enum Estado {
        /** Se recibió el archivo y todavía se está procesando. */
        EN_CURSO,

        /** Procesó. Puede tener rechazos y pendientes; eso no la hace fallida. */
        TERMINADA,

        /**
         * El archivo no coincidía con el perfil y no se importó <b>nada</b>.
         * Es distinto de terminar con rechazos: acá el problema es del perfil o
         * del archivo entero, no de filas sueltas.
         */
        RECHAZADA
    }

    public Importacion {
        desajustes = desajustes == null ? List.of() : List.copyOf(desajustes);
    }

    public static Importacion enCurso(String id, PerfilDeImportacion perfil, YearMonth periodo, LocalDate corte) {
        return new Importacion(id, perfil.nombre(), perfil.metrica(), periodo, corte,
                Estado.EN_CURSO, Informe.vacio(), List.of(), Instant.now(), null);
    }

    public Importacion terminada(Informe informe) {
        return new Importacion(id, perfil, metrica, periodo, fechaDeCorte,
                Estado.TERMINADA, informe, List.of(), iniciada, Instant.now());
    }

    public Importacion rechazada(List<Desajuste> desajustes) {
        return new Importacion(id, perfil, metrica, periodo, fechaDeCorte,
                Estado.RECHAZADA, Informe.vacio(), desajustes, iniciada, Instant.now());
    }
}
