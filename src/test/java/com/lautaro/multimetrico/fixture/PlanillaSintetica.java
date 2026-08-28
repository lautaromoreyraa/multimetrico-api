package com.lautaro.multimetrico.fixture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Arma planillas de prueba en memoria.
 *
 * <p>Se generan en tiempo de ejecución y nunca se guardan en disco. La razón no
 * es comodidad: el {@code .gitignore} del proyecto bloquea {@code *.xlsx} para
 * que ninguna planilla de una operación real entre al repositorio, y versionar un
 * fixture binario sería abrirle la puerta justo a lo que esa regla evita. Además
 * un archivo que se lee no dice qué está probando; este constructor sí.
 *
 * <p>Todos los datos son inventados.
 */
public final class PlanillaSintetica {

    private final XSSFWorkbook libro = new XSSFWorkbook();
    private final Sheet hoja;
    private int proximaFila = 0;

    private PlanillaSintetica(String nombreDeHoja) {
        this.hoja = libro.createSheet(nombreDeHoja);
    }

    public static PlanillaSintetica hoja(String nombre) {
        return new PlanillaSintetica(nombre);
    }

    /** Una fila de texto. Sirve para encabezados y para valores mal cargados. */
    public PlanillaSintetica fila(String... celdas) {
        Row fila = hoja.createRow(proximaFila++);
        for (int c = 0; c < celdas.length; c++) {
            if (celdas[c] != null) {
                fila.createCell(c).setCellValue(celdas[c]);
            }
        }
        return this;
    }

    /** Asesor, valor numérico y líder: la fila que se espera importar. */
    public PlanillaSintetica filaConValor(String asesor, double valor, String lider) {
        Row fila = hoja.createRow(proximaFila++);
        fila.createCell(0).setCellValue(asesor);
        fila.createCell(1).setCellValue(valor);
        if (lider != null) {
            fila.createCell(2).setCellValue(lider);
        }
        return this;
    }

    /**
     * Una celda con formato de porcentaje: Excel guarda {@code 0.805} y muestra
     * {@code 80,5%}. Es el caso que, sin detectarlo, importa el archivo cien veces
     * más chico.
     */
    public PlanillaSintetica filaConPorcentaje(String asesor, double fraccion, String lider) {
        Row fila = hoja.createRow(proximaFila++);
        fila.createCell(0).setCellValue(asesor);

        Cell celda = fila.createCell(1);
        celda.setCellValue(fraccion);
        CellStyle estilo = libro.createCellStyle();
        estilo.setDataFormat(libro.createDataFormat().getFormat("0.0%"));
        celda.setCellStyle(estilo);

        if (lider != null) {
            fila.createCell(2).setCellValue(lider);
        }
        return this;
    }

    /** Una fila completamente en blanco, como las que arrastran las planillas al final. */
    public PlanillaSintetica filaVacia() {
        hoja.createRow(proximaFila++);
        return this;
    }

    public byte[] bytes() {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            libro.write(salida);
            libro.close();
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** La planilla típica: encabezados en la fila 1 y datos debajo. */
    public static byte[] deMetrica(String nombreDeHoja, List<String> encabezados) {
        return hoja(nombreDeHoja).fila(encabezados.toArray(String[]::new)).bytes();
    }
}
