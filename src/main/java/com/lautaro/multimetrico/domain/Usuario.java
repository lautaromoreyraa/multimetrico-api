package com.lautaro.multimetrico.domain;

import com.lautaro.multimetrico.enumeration.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;

/**
 * Quién entra al sistema.
 *
 * <h2>El legajo es la identidad</h2>
 *
 * <p>No hay un "usuario" aparte del legajo. La pregunta que la aplicación
 * contesta en cada request es <b>de quién es esta fila</b>, y esa se contesta con
 * el legajo; un identificador distinto obligaría a traducir en cada control de
 * acceso, que es justo donde no conviene tener un paso de más.
 *
 * <p>{@code legajoClave} es el legajo en mayúsculas y existe por la misma razón
 * que en {@link Medicion}: la unicidad y la búsqueda se resuelven sobre una
 * columna ya normalizada en Java, en vez de depender de la collation del motor
 * —que no es la misma en MySQL que en H2, y volvería la regla dependiente de
 * dónde corre—.
 *
 * <h2>El lider sólo significa algo en un SUPERVISOR</h2>
 *
 * <p>Es el mismo texto que {@code Asesor.lider}, que es como el padrón nombra a
 * los líderes: así {@code ControlDeAcceso} puede preguntar "¿este equipo es
 * tuyo?" comparando contra el padrón sin ninguna tabla intermedia.
 *
 * <p>En un ASESOR o un ADMIN queda en {@code null}. No se valida acá que un
 * SUPERVISOR lo tenga: un supervisor sin equipo asignado es un usuario que
 * todavía no ve nada, no un registro corrupto, y fallar al leerlo de la base
 * sería peor que devolverle una lista vacía.
 *
 * <h2>La contraseña sale de acá hasheada o no sale</h2>
 *
 * <p>{@code password} guarda el resultado de BCrypt, nunca el texto. No hay
 * ningún camino en el código que la escriba en claro: el único que la setea es
 * {@code UsuarioService}, que hashea antes.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legajo", nullable = false, length = 40)
    private String legajo;

    @Column(name = "legajo_clave", nullable = false, length = 40)
    private String legajoClave;

    @Column(name = "nombre", nullable = false, length = 160)
    private String nombre;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    @Column(name = "lider", length = 160)
    private String lider;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    /** JPA. */
    protected Usuario() {
    }

    public Usuario(String legajo, String nombre, String passwordHasheada, Rol rol, String lider) {
        this.legajo = legajo;
        this.legajoClave = clave(legajo);
        this.nombre = nombre;
        this.password = passwordHasheada;
        this.rol = rol;
        this.lider = lider;
        this.activo = true;
        this.creadoEn = Instant.now();
    }

    /**
     * La forma normalizada de un legajo, para buscar y para la unique.
     *
     * <p>Estática y pública porque el repositorio la necesita para armar la
     * consulta: quien busca tiene que normalizar igual que quien guardó, y
     * teniendo una sola función no hay forma de que se separen.
     */
    public static String clave(String legajo) {
        return legajo == null ? null : legajo.trim().toUpperCase(Locale.ROOT);
    }

    public Long id() {
        return id;
    }

    public String legajo() {
        return legajo;
    }

    public String nombre() {
        return nombre;
    }

    public String password() {
        return password;
    }

    public Rol rol() {
        return rol;
    }

    public String lider() {
        return lider;
    }

    public boolean activo() {
        return activo;
    }

    public Instant creadoEn() {
        return creadoEn;
    }

    public void cambiarPassword(String passwordHasheada) {
        this.password = passwordHasheada;
    }

    public void desactivar() {
        this.activo = false;
    }
}
