package com.lautaro.multimetrico.persistencia.medicion;

import com.lautaro.multimetrico.medicion.MedicionService;
import com.lautaro.multimetrico.medicion.domain.Medicion;
import com.lautaro.multimetrico.persistencia.Decimales;
import com.lautaro.multimetrico.pliego.domain.Metrica;
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

/**
 * Una medición como fila.
 *
 * <p><b>El id es el orden de carga.</b> No es una clave técnica cualquiera: es el
 * dato que decide el desempate entre dos mediciones del mismo corte, que gana la
 * última cargada porque es la corrección. Ver {@link MedicionesJpa}.
 */
@Entity
@Table(name = "medicion")
public class MedicionEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legajo", nullable = false, length = 40)
    private String legajo;

    /**
     * El legajo en mayúsculas, que es por donde se busca.
     *
     * <p>Los archivos de origen escriben el mismo legajo como {@code u100} y como
     * {@code U100}, y el dominio los trata como el mismo
     * ({@code Medicion.esDe} compara con {@code equalsIgnoreCase}). Reproducir eso
     * con la collation del motor haría que la búsqueda distinga mayúsculas en H2
     * y no en MySQL; con la columna, se comporta igual en los dos.
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

    protected MedicionEntidad() {
    }

    static MedicionEntidad de(Medicion medicion) {
        MedicionEntidad entidad = new MedicionEntidad();
        entidad.legajo = medicion.legajo();
        entidad.legajoClave = MedicionService.clave(medicion.legajo());
        entidad.metrica = medicion.metrica();
        entidad.periodo = medicion.periodo();
        entidad.valor = medicion.valor();
        entidad.fechaDeCorte = medicion.fechaDeCorte();
        entidad.origenImportacion = medicion.origenImportacion();
        return entidad;
    }

    Medicion aDominio() {
        return new Medicion(legajo, metrica, periodo, Decimales.comoEntro(valor), fechaDeCorte, origenImportacion);
    }
}
