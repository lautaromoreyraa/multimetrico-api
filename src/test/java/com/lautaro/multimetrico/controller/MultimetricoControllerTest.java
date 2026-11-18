package com.lautaro.multimetrico.controller;

import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.CAMPANIA;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.mediciones;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.padron;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.pliego;
import static com.lautaro.multimetrico.fixture.Identidades.admin;
import static com.lautaro.multimetrico.fixture.Identidades.como;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lautaro.multimetrico.service.medicion.MedicionService;
import com.lautaro.multimetrico.service.padron.PadronService;
import com.lautaro.multimetrico.service.pliego.PliegoService;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class MultimetricoControllerTest {

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
            mockMvc = MockMvcBuilders.webAppContextSetup(contexto)
                    .apply(SecurityMockMvcConfigurers.springSecurity())
                    .build();
        }
        return mockMvc;
    }

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
                .param("lider", "PERALTA SILVIA").param("campania", CAMPANIA).with(como(admin())))
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

    @Test
    void cadaMetricaSaleConSuPropioActualizadoAl() throws Exception {
        mvc().perform(get("/api/multimetrico/2028-03/asesores/u100").param("campania", CAMPANIA).with(como(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("AGUIRRE FLAVIA YAMILA"))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='FCR')].actualizadoAl").value("2028-03-22"))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='TMO')].actualizadoAl").value("2028-03-19"))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='NPS')].actualizadoAl").value("2028-03-22"));
    }

    @Test
    void cadaMetricaDiceCuantoLeFaltaParaElTramoDeArriba() throws Exception {
        mvc().perform(get("/api/multimetrico/2028-03/asesores/u100").param("campania", CAMPANIA).with(como(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricas[?(@.metrica=='TMO')].tramoDeArriba.objetivo").value(600))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='TMO')].tramoDeArriba.distancia").value(12))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='TMO')].tramoDeArriba.condicion").value("BAJAR_DE"))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='TMO')].tramoDeArriba.detalle",
                        org.hamcrest.Matchers.contains("hay que quedar por debajo de 600 —sobran 12— y sumar 2")))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='FCR')].tramoDeArriba[0]").doesNotExist());
    }

    @Test
    void cadaMetricaSaleConSuUnidad() throws Exception {
        mvc().perform(get("/api/multimetrico/2028-03/asesores/u100").param("campania", CAMPANIA).with(como(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricas[?(@.metrica=='TMO')].unidad").value("SEGUNDOS"))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='FCR')].unidad").value("PORCENTAJE"))
                .andExpect(jsonPath("$.metricas[?(@.metrica=='NPS')].unidad").value("PORCENTAJE"));
    }

    @Test
    void sinPliegoDelPeriodoResponde404DiciendoQueFaltaElPliego() throws Exception {
        mvc().perform(get("/api/multimetrico/2028-09").param("campania", CAMPANIA).with(como(admin())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("No hay pliego cargado")));
    }

    @Test
    void unLiderQueNoEstaResponde404ConLosLideresQueSiEstan() throws Exception {
        mvc().perform(get("/api/multimetrico/2028-03")
                .param("lider", "CABRERA MONICA").param("campania", CAMPANIA).with(como(admin())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("PERALTA SILVIA")));
    }
}
