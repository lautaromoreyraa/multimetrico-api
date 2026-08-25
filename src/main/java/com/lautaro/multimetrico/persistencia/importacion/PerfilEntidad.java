package com.lautaro.multimetrico.persistencia.importacion;

import com.lautaro.multimetrico.importacion.domain.FormatoDeNumero;
import com.lautaro.multimetrico.importacion.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.pliego.domain.Metrica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Locale;

/**
 * El perfil de importación como fila.
 *
 * <p>Que el perfil sea una fila y no una clase es todo el punto del módulo: el
 * formato de los archivos de origen es <b>dato, no código</b>. Cuando la empresa
 * renombre una columna se edita un registro; no se recompila nada.
 *
 * <p>El {@code FormatoDeNumero} se aplana en dos columnas en vez de ir como
 * objeto embebido con nombres inventados: son dos enums y así se ven en un
 * {@code SELECT}.
 */
@Entity
@Table(name = "perfil_de_importacion")
public class PerfilEntidad {

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

    /** {@code null} significa "cualquier hoja", igual que en el dominio. */
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

    @Enumerated(EnumType.STRING)
    @Column(name = "separador_decimal", nullable = false, length = 20)
    private FormatoDeNumero.SeparadorDecimal separadorDecimal;

    @Enumerated(EnumType.STRING)
    @Column(name = "escala_de_numero", nullable = false, length = 20)
    private FormatoDeNumero.Escala escalaDeNumero;

    protected PerfilEntidad() {
    }

    static String clave(String nombre) {
        return nombre.trim().toUpperCase(Locale.ROOT);
    }

    void copiarDe(PerfilDeImportacion perfil) {
        this.nombre = perfil.nombre();
        this.nombreClave = clave(perfil.nombre());
        this.metrica = perfil.metrica();
        this.hoja = perfil.hoja();
        this.filaEncabezados = perfil.filaEncabezados();
        this.columnaAsesor = perfil.columnaAsesor();
        this.columnaValor = perfil.columnaValor();
        this.columnaLider = perfil.columnaLider();
        this.separadorDecimal = perfil.formatoNumero().separadorDecimal();
        this.escalaDeNumero = perfil.formatoNumero().escala();
    }

    static PerfilEntidad de(PerfilDeImportacion perfil) {
        PerfilEntidad entidad = new PerfilEntidad();
        entidad.copiarDe(perfil);
        return entidad;
    }

    PerfilDeImportacion aDominio() {
        return new PerfilDeImportacion(nombre, metrica, hoja, filaEncabezados, columnaAsesor, columnaValor,
                columnaLider, new FormatoDeNumero(separadorDecimal, escalaDeNumero));
    }
}
