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

    public boolean cualquierHoja() {
        return hoja == null;
    }

    public int filaEncabezadosEnPoi() {
        return filaEncabezados - 1;
    }

    private static String vacioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
