package com.lautaro.multimetrico.domain;

import com.lautaro.multimetrico.enumeration.Metrica;
import com.lautaro.multimetrico.util.DecimalUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

/**
 * El valor que una métrica tenía para un asesor a una fecha de corte.
 *
 * <p><b>Es un hecho histórico, no un casillero que se pisa.</b> La planilla que
 * este proyecto reemplaza tiene una celda por asesor y por métrica: cuando llega
 * el archivo del lunes, se pega encima del que estaba y el número anterior deja
 * de existir. Entonces, cuando un asesor pregunta por qué su FCR bajó dos puntos
 * entre el martes y el jueves, no hay con qué contestarle — el número viejo no
 * está en ningún lado.
 *
 * <p>Acá cada carga semanal es una medición <em>nueva</em>. Dentro del mes queda
 * la serie entera, y de ahí salen las dos cosas que la planilla no puede dar: la
 * evolución dentro del mes, y la explicación de por qué un número cambió. Por eso
 * esta entidad no tiene ni un solo mutador: se inserta y se lee, nunca se
 * actualiza. Corregir un número es cargar otra medición del mismo corte.
 *
 * <p><b>El id es el orden de carga</b>, y eso es parte del contrato. Es el dato
 * que decide el desempate entre dos mediciones del mismo corte, que gana la
 * última cargada porque es la corrección. Ver {@code MedicionRepository}.
 *
 * <p>La {@code fechaDeCorte} es el dato que hace posible todo lo demás y es el
 * que la planilla pierde. <b>Cada métrica tiene su propio corte</b>: es normal
 * tener el TMO al viernes pasado y el FCR al lunes anterior, porque los archivos
 * de origen se publican por separado. El multimétrico las junta, y si no dice a
 * qué fecha corresponde cada una, está mostrando una foto que nunca existió.
 *
 * <p>Ojo con confundir {@code periodo} y {@code fechaDeCorte}. El período es el
 * mes que se está midiendo —el que elige el pliego con el que se evalúa—; el
 * corte es el día hasta el que el archivo acumuló. No tienen por qué caer en el
 * mismo mes: el cierre de agosto suele llegar en los primeros días de septiembre.
 */
@Entity
@Table(name = "medicion")
public class Medicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legajo", nullable = false, length = 40)
    private String legajo;

    /**
     * El legajo en mayúsculas, que es por donde se busca.
     *
     * <p>Los archivos de origen escriben el mismo legajo como {@code u100} y como
     * {@code U100}, y el dominio los trata como el mismo ({@link #esDe} compara
     * sin distinguir mayúsculas). Reproducir eso con la collation del motor haría
     * que la búsqueda distinga mayúsculas en H2 y no en MySQL; con la columna, se
     * comporta igual en los dos.
     */
    @Column(name = "legajo_clave", nullable = false, length = 40)
    private String legajoClave;

    @Enumerated(EnumType.STRING)
    @Column(name = "metrica", nullable = false, length = 40)
    private Metrica metrica;

    @Column(name = "periodo", nullable = false, length = 7)
    private YearMonth periodo;

    /**
     * {@code DECIMAL(18,6)}, con escala explícita.
     *
     * <p>Un {@code DECIMAL} sin escala en MySQL es {@code DECIMAL(10,0)}: un FCR
     * de 80,04 se guardaría como 80 y caería en otra banda. No falla nada, no se
     * ve en ningún log; simplemente el alcance da distinto.
     */
    @Column(name = "valor", nullable = false, precision = 18, scale = 6)
    private BigDecimal valor;

    @Column(name = "fecha_de_corte", nullable = false)
    private LocalDate fechaDeCorte;

    /**
     * El id de la importación que la trajo, como texto y sin {@code @ManyToOne}.
     *
     * <p>Una medición cargada a mano por la API no tiene origen, y borrar el
     * historial de importaciones no puede llevarse puestas las mediciones que
     * produjo: la medición es un hecho por sí misma y la importación es su
     * comprobante. Una asociación gestionada por JPA ataría el ciclo de vida de
     * una al de la otra.
     */
    @Column(name = "origen_importacion", length = 36)
    private String origenImportacion;

    protected Medicion() {
    }

    public Medicion(String legajo, Metrica metrica, YearMonth periodo, BigDecimal valor,
            LocalDate fechaDeCorte, String origenImportacion) {

        if (legajo == null || legajo.isBlank()) {
            throw new IllegalArgumentException("La medición necesita un legajo");
        }
        if (metrica == null) {
            throw new IllegalArgumentException("La medición de %s necesita una métrica".formatted(legajo));
        }
        if (periodo == null) {
            throw new IllegalArgumentException(
                    "La medición de %s en %s necesita un período".formatted(legajo, metrica));
        }
        // Un valor nulo no es un cero: es una métrica que no llegó, y eso se
        // representa no cargando la medición, no cargándola vacía.
        if (valor == null) {
            throw new IllegalArgumentException(
                    "La medición de %s en %s necesita un valor. Una métrica sin dato no se carga en cero: "
                            .formatted(legajo, metrica)
                            + "se deja faltante.");
        }
        if (fechaDeCorte == null) {
            throw new IllegalArgumentException(
                    ("La medición de %s en %s necesita una fecha de corte. Sin ella no se sabe cuál de dos "
                            + "cargas es la vigente ni a qué día corresponde el número.").formatted(legajo, metrica));
        }
        this.legajo = legajo.trim();
        this.legajoClave = clave(legajo);
        this.metrica = metrica;
        this.periodo = periodo;
        this.valor = valor;
        this.fechaDeCorte = fechaDeCorte;
        this.origenImportacion = origenImportacion;
    }

    /** El legajo como se lo busca: sin espacios y en mayúsculas. */
    public static String clave(String legajo) {
        return legajo.trim().toUpperCase(Locale.ROOT);
    }

    public Long id() {
        return id;
    }

    public String legajo() {
        return legajo;
    }

    public String legajoClave() {
        return legajoClave;
    }

    public Metrica metrica() {
        return metrica;
    }

    public YearMonth periodo() {
        return periodo;
    }

    public BigDecimal valor() {
        return DecimalUtil.comoEntro(valor);
    }

    public LocalDate fechaDeCorte() {
        return fechaDeCorte;
    }

    public String origenImportacion() {
        return origenImportacion;
    }

    /**
     * Si esta medición desplaza a {@code otra}, que es la que venía valiendo.
     *
     * <p>Gana la del corte más reciente. <b>Empatado el corte, gana ésta</b>, y
     * el criterio no es arbitrario: dos mediciones con la misma fecha de corte
     * son dos cargas del mismo archivo, y la segunda es una corrección de la
     * primera. Quedarse con la más vieja dejaría sin efecto justamente la
     * corrección que alguien se tomó el trabajo de subir.
     *
     * <p>Eso significa algo solo si quien recorre la historia lo hace <b>en
     * orden de carga</b>, tratando cada medición como candidata a desplazar a la
     * que quedó atrás. El repositorio devuelve la historia en ese orden.
     */
    public boolean desplazaA(Medicion otra) {
        return !fechaDeCorte.isBefore(otra.fechaDeCorte());
    }

    public boolean esDe(String otroLegajo) {
        return legajo.equalsIgnoreCase(otroLegajo == null ? "" : otroLegajo.trim());
    }
}
