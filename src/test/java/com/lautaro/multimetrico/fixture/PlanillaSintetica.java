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

    public PlanillaSintetica fila(String... celdas) {
        Row fila = hoja.createRow(proximaFila++);
        for (int c = 0; c < celdas.length; c++) {
            if (celdas[c] != null) {
                fila.createCell(c).setCellValue(celdas[c]);
            }
        }
        return this;
    }

    public PlanillaSintetica filaConValor(String asesor, double valor, String lider) {
        Row fila = hoja.createRow(proximaFila++);
        fila.createCell(0).setCellValue(asesor);
        fila.createCell(1).setCellValue(valor);
        if (lider != null) {
            fila.createCell(2).setCellValue(lider);
        }
        return this;
    }

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

    public static byte[] deMetrica(String nombreDeHoja, List<String> encabezados) {
        return hoja(nombreDeHoja).fila(encabezados.toArray(String[]::new)).bytes();
    }
}
