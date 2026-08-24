package com.lautaro.multimetrico.padron.domain;

/**
 * Una persona del padrón.
 *
 * <p>El {@code legajo} es la identidad. Es el único dato estable que atraviesa
 * todos los sistemas —el nombre se escribe distinto en cada archivo y cambia
 * cuando alguien se casa—, así que todo lo demás cuelga de él. Los archivos de
 * métricas no lo traen, y por eso hace falta resolverlos por nombre; el punto de
 * este record es que apenas se resuelve una fila, deja de depender del texto.
 *
 * <p>El {@code lider} es por lo que hoy cada supervisor filtra a mano, todas las
 * semanas, en cada archivo de cada métrica.
 */
public record Asesor(String legajo, String nombre, String lider) {

    public Asesor {
        if (legajo == null || legajo.isBlank()) {
            throw new IllegalArgumentException("El asesor necesita un legajo");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El asesor %s necesita un nombre".formatted(legajo));
        }
        if (lider == null || lider.isBlank()) {
            throw new IllegalArgumentException("El asesor %s necesita un líder".formatted(legajo));
        }
        legajo = legajo.trim();
    }

    public NombreNormalizado nombreNormalizado() {
        return NombreNormalizado.de(nombre);
    }

    public NombreNormalizado liderNormalizado() {
        return NombreNormalizado.de(lider);
    }
}
