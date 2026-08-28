package com.lautaro.multimetrico.domain;

import com.lautaro.multimetrico.enumeration.Metrica;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Locale;

/**
 * Dónde está cada cosa dentro de una planilla de origen.
 *
 * <p><b>Nadie de este proyecto tiene acceso a esos archivos, ni los va a tener.</b>
 * No se sabe qué columnas traen, en qué orden, con qué encabezados ni en qué
 * hoja. Y aunque se supiera, la empresa que los publica los cambia sin avisar.
 * Escribir el mapeo en el código significaría que cada cambio de encabezado es
 * un release; que el perfil sea una <em>fila</em> significa que es un registro
 * que se edita. Esa es la decisión que sostiene el módulo entero: el formato de
 * los archivos de origen es <b>dato, no código</b>.
 *
 * <h2>Las columnas se nombran por su encabezado, no por su posición</h2>
 *
 * <p>Podría guardarse el número de columna, que es lo que la inspección muestra
 * primero. No se hace, porque una posición no es una identidad: el día que la
 * empresa inserte una columna al principio, todas las demás se corren y un
 * perfil por posición seguiría importando —leyendo la columna de al lado— sin
 * que nada falle. Eso es exactamente lo que hace la planilla que este proyecto
 * reemplaza. Con el encabezado como identidad, una columna que se mueve se
 * vuelve a encontrar sola, y una que se renombra frena la importación entera.
 *
 * <p>El costo de la decisión, dicho para que no sorprenda: una columna
 * <b>sin encabezado</b> no se puede referenciar. Hoy no hay forma de perfilarla.
 *
 * <h2>Detalles que se pagan caro si se invierten</h2>
 *
 * <ul>
 *   <li>{@code filaEncabezados} va <b>en base 1, como la numera Excel</b>. Todo
 *       número de fila que sale de este módulo —sobre todo el de un rechazo—
 *       tiene que poder tipearse en el cuadro de "ir a" de Excel y caer en la
 *       fila que el mensaje describe. Un rechazo cuyo número está corrido en uno
 *       hace perder más tiempo que no informarlo.</li>
 *   <li>{@code hoja} en {@code null} es "la primera". Los archivos de una sola
 *       hoja son la mayoría y obligarlos a nombrarla no aporta nada.</li>
 *   <li>{@code columnaLider} es opcional. Cuando está, no decide nada: el líder
 *       de cada asesor lo pone el padrón. Sirve como control cruzado — si el
 *       archivo dice un líder y el padrón dice otro, hay algo para mirar.</li>
 *   <li>{@code columnaAsesor} puede traer el legajo en vez del nombre. No hace
 *       falta un campo aparte: el resolutor prueba primero por legajo, así que
 *       una columna que traiga legajos resuelve por la vía más segura sin que el
 *       perfil tenga que declararlo.</li>
 * </ul>
 *
 * <p>Un perfil se puede reemplazar cargándolo de nuevo con el mismo nombre, como
 * el padrón y a diferencia del pliego. Es configuración de trabajo: se corrige
 * hasta que el archivo entra. Que reemplazarlo no reescriba la historia lo
 * garantiza {@link Importacion}, que se queda con el nombre del perfil con el
 * que efectivamente se procesó.
 */
@Entity
@Table(name = "perfil_de_importacion")
public class PerfilDeImportacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "nombre_clave", nullable = false, length = 120, unique = true)
    private String nombreClave;

    @Enumerated(EnumType.STRING)
    @Column(name = "metrica", nullable = false, length = 40)
    private Metrica metrica;

    /** {@code null} significa "cualquier hoja". */
    @Column(name = "hoja", length = 160)
    private String hoja;

    @Column(name = "fila_encabezados", nullable = false)
    private int filaEncabezados;

    @Column(name = "columna_asesor", nullable = false, length = 160)
    private String columnaAsesor;

    @Column(name = "columna_valor", nullable = false, length = 160)
    private String columnaValor;

    @Column(name = "columna_lider", length = 160)
    private String columnaLider;

    @Embedded
    private FormatoDeNumero formatoNumero;

    protected PerfilDeImportacion() {
    }

    public PerfilDeImportacion(String nombre, Metrica metrica, String hoja, int filaEncabezados,
            String columnaAsesor, String columnaValor, String columnaLider, FormatoDeNumero formatoNumero) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El perfil necesita un nombre");
        }
        if (metrica == null) {
            throw new IllegalArgumentException(
                    "El perfil %s necesita decir qué métrica trae el archivo".formatted(nombre));
        }
        if (filaEncabezados < 1) {
            throw new IllegalArgumentException(
                    ("La fila de encabezados del perfil %s se numera como en Excel, desde 1; "
                            + "llegó %d").formatted(nombre, filaEncabezados));
        }
        if (columnaAsesor == null || columnaAsesor.isBlank()) {
            throw new IllegalArgumentException(
                    "El perfil %s necesita el encabezado de la columna del asesor".formatted(nombre));
        }
        if (columnaValor == null || columnaValor.isBlank()) {
            throw new IllegalArgumentException(
                    "El perfil %s necesita el encabezado de la columna del valor".formatted(nombre));
        }
        this.nombre = nombre.trim();
        this.nombreClave = clave(nombre);
        this.metrica = metrica;
        this.hoja = vacioComoNulo(hoja);
        this.filaEncabezados = filaEncabezados;
        this.columnaAsesor = columnaAsesor.trim();
        this.columnaValor = columnaValor.trim();
        this.columnaLider = vacioComoNulo(columnaLider);
        this.formatoNumero = formatoNumero == null ? FormatoDeNumero.estandar() : formatoNumero;
    }

    public static String clave(String nombre) {
        return nombre.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Guardar un perfil con un nombre que ya existe lo edita, no lo duplica.
     *
     * <p>Es la operación normal del módulo: la empresa cambia el reporte y
     * alguien corrige el nombre de una columna. Un alta a secas chocaría con la
     * unique del nombre y obligaría a borrar antes de corregir.
     */
    public void actualizarCon(PerfilDeImportacion otro) {
        this.nombre = otro.nombre;
        this.nombreClave = otro.nombreClave;
        this.metrica = otro.metrica;
        this.hoja = otro.hoja;
        this.filaEncabezados = otro.filaEncabezados;
        this.columnaAsesor = otro.columnaAsesor;
        this.columnaValor = otro.columnaValor;
        this.columnaLider = otro.columnaLider;
        this.formatoNumero = otro.formatoNumero;
    }

    public Long id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String nombreClave() {
        return nombreClave;
    }

    public Metrica metrica() {
        return metrica;
    }

    public String hoja() {
        return hoja;
    }

    public int filaEncabezados() {
        return filaEncabezados;
    }

    public String columnaAsesor() {
        return columnaAsesor;
    }

    public String columnaValor() {
        return columnaValor;
    }

    public String columnaLider() {
        return columnaLider;
    }

    public FormatoDeNumero formatoNumero() {
        return formatoNumero;
    }

    public boolean traeLider() {
        return columnaLider != null;
    }

    /** La primera hoja del libro cuando el perfil no nombra ninguna. */
    public boolean cualquierHoja() {
        return hoja == null;
    }

    /** El índice de la fila de encabezados como lo cuenta POI, en base 0. */
    public int filaEncabezadosEnPoi() {
        return filaEncabezados - 1;
    }

    private static String vacioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
