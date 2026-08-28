package com.lautaro.multimetrico.service.planilla.impl;

import com.lautaro.multimetrico.domain.PerfilDeImportacion;
import com.lautaro.multimetrico.dto.importacion.Desajuste;
import com.lautaro.multimetrico.dto.planilla.FilaLeida;
import com.lautaro.multimetrico.dto.planilla.ValorCrudo;
import com.lautaro.multimetrico.exception.PlanillaRechazadaException;
import com.lautaro.multimetrico.service.planilla.LectorDePlanilla;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

/**
 * El lector de planillas de Excel, con POI.
 *
 * <p>Se llama {@code Excel} y no {@code Impl} a propósito: es <em>una</em> forma
 * de leer una fuente, no la única posible. El día que la empresa publique CSV, la
 * implementación nueva se va a llamar por su formato y las dos van a convivir sin
 * que ninguna tenga el nombre genérico.
 */
@Service
public class LectorDePlanillaExcel implements LectorDePlanilla {

    /**
     * Formatea la celda como se ve en pantalla. Importa para los rechazos: el que
     * lee el informe quiere que le digan lo que dice su archivo, no la
     * representación interna.
     */
    private final DataFormatter formateador = new DataFormatter(Locale.of("es", "AR"));

    @Override
    public List<FilaLeida> leer(InputStream entrada, PerfilDeImportacion perfil) {
        try (Workbook libro = WorkbookFactory.create(entrada)) {
            Sheet hoja = ubicarHoja(libro, perfil);
            Map<String, Integer> columnas = ubicarColumnas(hoja, perfil);
            return leerFilas(hoja, perfil, columnas);
        } catch (IOException e) {
            throw new PlanillaRechazadaException(List.of(Desajuste.archivoIlegible(e.getMessage())));
        }
    }

    @Override
    public List<List<String>> muestra(InputStream entrada, String hoja, int filaEncabezados, int ejemplos) {
        try (Workbook libro = WorkbookFactory.create(entrada)) {
            Sheet elegida = hoja == null || hoja.isBlank() ? libro.getSheetAt(0) : libro.getSheet(hoja);
            if (elegida == null) {
                throw new PlanillaRechazadaException(List.of(Desajuste.hoja(hoja, nombresDeHojas(libro))));
            }
            Row encabezados = elegida.getRow(filaEncabezados - 1);
            if (encabezados == null) {
                throw new PlanillaRechazadaException(List.of(
                        Desajuste.filaDeEncabezados(filaEncabezados, elegida.getLastRowNum() + 1)));
            }

            List<List<String>> porColumna = new ArrayList<>();
            for (int c = 0; c < encabezados.getLastCellNum(); c++) {
                List<String> columna = new ArrayList<>();
                columna.add(texto(encabezados.getCell(c)));
                for (int f = filaEncabezados; f < filaEncabezados + ejemplos; f++) {
                    Row fila = elegida.getRow(f);
                    columna.add(fila == null ? "" : texto(fila.getCell(c)));
                }
                porColumna.add(List.copyOf(columna));
            }
            return List.copyOf(porColumna);
        } catch (IOException e) {
            throw new PlanillaRechazadaException(List.of(Desajuste.archivoIlegible(e.getMessage())));
        }
    }

    @Override
    public List<String> hojasDe(InputStream entrada) {
        try (Workbook libro = WorkbookFactory.create(entrada)) {
            return nombresDeHojas(libro);
        } catch (IOException e) {
            throw new PlanillaRechazadaException(List.of(Desajuste.archivoIlegible(e.getMessage())));
        }
    }

    private Sheet ubicarHoja(Workbook libro, PerfilDeImportacion perfil) {
        if (perfil.cualquierHoja()) {
            return libro.getSheetAt(0);
        }
        Sheet hoja = libro.getSheet(perfil.hoja());
        if (hoja == null) {
            throw new PlanillaRechazadaException(List.of(Desajuste.hoja(perfil.hoja(), nombresDeHojas(libro))));
        }
        return hoja;
    }

    /**
     * Busca cada columna del perfil por su encabezado y junta <b>todos</b> los
     * desajustes antes de rendirse.
     *
     * <p>Un encabezado repetido también es desajuste: si la hoja trae dos columnas
     * llamadas igual, elegir la primera es adivinar, y adivinar acá significa
     * importar los números de otra cosa.
     */
    private Map<String, Integer> ubicarColumnas(Sheet hoja, PerfilDeImportacion perfil) {
        Row encabezados = hoja.getRow(perfil.filaEncabezadosEnPoi());
        if (encabezados == null) {
            throw new PlanillaRechazadaException(List.of(
                    Desajuste.filaDeEncabezados(perfil.filaEncabezados(), hoja.getLastRowNum() + 1)));
        }

        Map<String, List<Integer>> porEncabezado = new LinkedHashMap<>();
        List<String> presentes = new ArrayList<>();
        for (int c = 0; c < encabezados.getLastCellNum(); c++) {
            String encabezado = texto(encabezados.getCell(c));
            presentes.add(encabezado);
            if (!encabezado.isBlank()) {
                porEncabezado.computeIfAbsent(normalizar(encabezado), k -> new ArrayList<>()).add(c);
            }
        }

        List<Desajuste> desajustes = new ArrayList<>();
        Map<String, Integer> ubicadas = new LinkedHashMap<>();
        ubicar("asesor", perfil.columnaAsesor(), porEncabezado, presentes, ubicadas, desajustes);
        ubicar("valor", perfil.columnaValor(), porEncabezado, presentes, ubicadas, desajustes);
        if (perfil.traeLider()) {
            ubicar("lider", perfil.columnaLider(), porEncabezado, presentes, ubicadas, desajustes);
        }

        if (!desajustes.isEmpty()) {
            throw new PlanillaRechazadaException(desajustes);
        }
        return ubicadas;
    }

    private void ubicar(String rol, String buscada, Map<String, List<Integer>> porEncabezado,
            List<String> presentes, Map<String, Integer> ubicadas, List<Desajuste> desajustes) {

        List<Integer> encontradas = porEncabezado.get(normalizar(buscada));
        if (encontradas == null) {
            desajustes.add(Desajuste.columna(rol, buscada, presentes));
        } else if (encontradas.size() > 1) {
            desajustes.add(Desajuste.columnaRepetida(rol, buscada,
                    encontradas.stream().map(String::valueOf).toList()));
        } else {
            ubicadas.put(rol, encontradas.getFirst());
        }
    }

    /**
     * Lee de la fila siguiente a los encabezados hasta el final.
     *
     * <p>Las filas totalmente vacías no se cuentan. Las planillas arrastran cientos
     * de filas en blanco al final —solo por haber tenido formato alguna vez— y
     * contarlas convertiría el informe en ruido: mil filas leídas y novecientas
     * rechazadas por venir vacías.
     */
    private List<FilaLeida> leerFilas(Sheet hoja, PerfilDeImportacion perfil, Map<String, Integer> columnas) {
        List<FilaLeida> filas = new ArrayList<>();
        Integer columnaLider = columnas.get("lider");

        for (int f = perfil.filaEncabezadosEnPoi() + 1; f <= hoja.getLastRowNum(); f++) {
            Row fila = hoja.getRow(f);
            if (fila == null) {
                continue;
            }
            FilaLeida leida = new FilaLeida(
                    f + 1,
                    texto(fila.getCell(columnas.get("asesor"))),
                    valor(fila.getCell(columnas.get("valor"))),
                    columnaLider == null ? null : texto(fila.getCell(columnaLider)));

            if (!leida.vacia()) {
                filas.add(leida);
            }
        }
        return List.copyOf(filas);
    }

    /**
     * El contenido de la celda, conservando si Excel la guardó como número y con
     * formato de porcentaje.
     *
     * <p>Esa distinción no es cosmética: una celda con formato de porcentaje
     * guarda 0,805 y muestra 80,5%. Sin saberlo, el mismo archivo se importa cien
     * veces más chico.
     */
    private ValorCrudo valor(Cell celda) {
        if (celda == null) {
            return ValorCrudo.ninguno();
        }
        CellType tipo = celda.getCellType() == CellType.FORMULA
                ? celda.getCachedFormulaResultType()
                : celda.getCellType();

        if (tipo == CellType.NUMERIC && !DateUtil.isCellDateFormatted(celda)) {
            String formato = celda.getCellStyle().getDataFormatString();
            boolean porcentaje = formato != null && formato.contains("%");
            return ValorCrudo.deNumero(celda.getNumericCellValue(), porcentaje, formateador.formatCellValue(celda));
        }
        return ValorCrudo.deTexto(formateador.formatCellValue(celda));
    }

    private String texto(Cell celda) {
        return celda == null ? "" : formateador.formatCellValue(celda).trim();
    }

    private List<String> nombresDeHojas(Workbook libro) {
        List<String> nombres = new ArrayList<>();
        for (int i = 0; i < libro.getNumberOfSheets(); i++) {
            nombres.add(libro.getSheetName(i));
        }
        return List.copyOf(nombres);
    }

    /**
     * Los encabezados se comparan sin distinguir mayúsculas ni espacios de más. Un
     * perfil no se tiene que romper porque alguien escribió "TMO " con un espacio
     * al final.
     */
    private String normalizar(String encabezado) {
        return encabezado == null ? "" : encabezado.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
