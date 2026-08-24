package com.lautaro.multimetrico.multimetrico;

import static com.lautaro.multimetrico.multimetrico.EscenarioDeEjemplo.CAMPANIA;
import static com.lautaro.multimetrico.multimetrico.EscenarioDeEjemplo.mediciones;
import static com.lautaro.multimetrico.multimetrico.EscenarioDeEjemplo.padron;
import static com.lautaro.multimetrico.multimetrico.EscenarioDeEjemplo.pliego;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lautaro.multimetrico.medicion.MedicionService;
import com.lautaro.multimetrico.padron.PadronService;
import com.lautaro.multimetrico.pliego.PliegoService;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class MultimetricoControllerTest {

    /** Período propio de esta clase, para no cruzarse con los otros tests. */
    private static final YearMonth PERIODO = YearMonth.of(2028, 3);

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private PliegoService pliegos;

    @Autowired
    private PadronService padrones;

    @Autowired
    private MedicionService medicionesDelMes;

    private static boolean cargado;

    private MockMvc mockMvc;

    private MockMvc mvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        }
        return mockMvc;
    }

    /**
     * El escenario se carga una sola vez: el pliego no se puede pisar y las
     * mediciones se acumulan, así que cargarlo por test daría 409 y duplicaría la
     * historia.
     */
    @BeforeEach
    void cargarEscenario() {
        if (cargado) {
            return;
        }
        pliegos.cargar(pliego(PERIODO));
        padrones.cargar(padron(PERIODO));
        medicionesDelMes.registrar(mediciones(PERIODO));
        cargado = true;
    }

    @Test
    void devuelveElEquipoConElResumenAdelante() throws Exception {
        mvc().perform(get("/api/multimetrico/2028-03")
                .param("lider", "PERALTA SILVIA").param("campania", CAMPANIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alcanceMaximo").value(10.5))
                .andExpect(jsonPath("$.resumen.asesores").value(2))
                .andExpect(jsonPath("$.resumen.completos").value(1))
                .andExpect(jsonPath("$.resumen.conFaltantes").value(1))
                .andExpect(jsonPath("$.resumen.actualizadoDesde").value("2028-03-19"))
                .andExpect(jsonPath("$.filas[0].legajo").value("u100"))
                .andExpect(jsonPath("$.filas[0].alcanceTotal").value(8.5))
                .andExpect(jsonPath("$.filas[1].faltantes[0]").value("NPS"));
    }

    /**
     * Lo que la planilla pierde: cada métrica sale con la fecha del archivo del
     * que salió, no con una sola fecha para toda la fila.
     */
    @Test
    void cadaMetricaSaleConSuPropioActualizadoAl() throws Exception {
        mvc().perform(get("/api/multimetrico/2028-03/asesores/u100").param("campania", CAMPANIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("AGUIRRE FLAVIA YAMILA"))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='FCR')].actualizadoAl").value("2028-03-22"))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='TMO')].actualizadoAl").value("2028-03-19"))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='NPS')].actualizadoAl").value("2028-03-22"));
    }

    /** El único dato accionable de la tabla: qué hacer para sumar más. */
    @Test
    void cadaMetricaDiceCuantoLeFaltaParaElTramoDeArriba() throws Exception {
        mvc().perform(get("/api/multimetrico/2028-03/asesores/u100").param("campania", CAMPANIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricas[?(@.metrica=='TMO')].tramoDeArriba.objetivo").value(600))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='TMO')].tramoDeArriba.distancia").value(12))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='TMO')].tramoDeArriba.condicion").value("BAJAR_DE"))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='TMO')].tramoDeArriba.detalle",
                        org.hamcrest.Matchers.contains("hay que quedar por debajo de 600 —sobran 12— y sumar 2")))
                // FCR ya está en la mejor banda: no hay tramo de arriba que mostrar.
                .andExpect(jsonPath("$.metricas[?(@.metrica=='FCR')].tramoDeArriba[0]").doesNotExist());
    }

    @Test
    void sinPliegoDelPeriodoResponde404DiciendoQueFaltaElPliego() throws Exception {
        mvc().perform(get("/api/multimetrico/2028-09").param("campania", CAMPANIA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("No hay pliego cargado")));
    }

    @Test
    void unLiderQueNoEstaResponde404ConLosLideresQueSiEstan() throws Exception {
        mvc().perform(get("/api/multimetrico/2028-03")
                .param("lider", "CABRERA MONICA").param("campania", CAMPANIA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("PERALTA SILVIA")));
    }
}
