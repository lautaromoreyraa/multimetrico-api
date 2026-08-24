package com.lautaro.multimetrico.medicion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * El contexto de Spring —y con él los repositorios en memoria— se comparte entre
 * clases y entre métodos de test. Como las mediciones se acumulan y nunca se
 * pisan, <b>cada test usa su propio período</b>: si dos compartieran uno, el
 * segundo vería la historia que dejó el primero.
 */
@SpringBootTest
class MedicionControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc;

    private MockMvc mvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        }
        return mockMvc;
    }

    private static String lote(String periodo, String corte, String filas) {
        return """
                { "periodo": "%s", "fechaDeCorte": "%s", "mediciones": [ %s ] }
                """.formatted(periodo, corte, filas);
    }

    private void registrar(String cuerpo) throws Exception {
        mvc().perform(post("/api/mediciones").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());
    }

    @Test
    void registraUnLoteYDevuelveElDesgloseDeLoQueEntro() throws Exception {
        String cuerpo = lote("2027-05", "2027-05-08", """
                { "legajo": "u100", "metrica": "FCR", "valor": 70.5 },
                { "legajo": "u200", "metrica": "FCR", "valor": 61.0 },
                { "legajo": "u100", "metrica": "TMO", "valor": 712 }
                """);

        mvc().perform(post("/api/mediciones").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registradas").value(3))
                .andExpect(jsonPath("$.asesores").value(2))
                .andExpect(jsonPath("$.porMetrica.FCR").value(2))
                .andExpect(jsonPath("$.cortes[0]").value("2027-05-08"));
    }

    /**
     * La segunda carga no pisa a la primera: las dos quedan y la historia dice
     * cuál se está usando. Es la conversación que hoy no se puede tener.
     */
    @Test
    void laHistoriaGuardaLasDosCargasYMarcaCualEsLaVigente() throws Exception {
        registrar(lote("2027-06", "2027-06-08", """
                { "legajo": "u100", "metrica": "FCR", "valor": 70.5 }
                """));
        registrar(lote("2027-06", "2027-06-15", """
                { "legajo": "u100", "metrica": "FCR", "valor": 78.2 }
                """));

        mvc().perform(get("/api/mediciones/2027-06").param("legajo", "u100").param("metrica", "FCR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.mediciones[0].valor").value(70.5))
                .andExpect(jsonPath("$.mediciones[0].vigente").value(false))
                .andExpect(jsonPath("$.mediciones[1].valor").value(78.2))
                .andExpect(jsonPath("$.mediciones[1].vigente").value(true));
    }

    /**
     * El lote lleva un corte, pero un ítem puede traer el suyo. Es lo que le
     * permite al importador subir dos archivos de cortes distintos en una sola
     * transacción en vez de partir el lote.
     */
    @Test
    void unItemPuedePisarLaFechaDeCorteDelLote() throws Exception {
        mvc().perform(post("/api/mediciones").contentType(MediaType.APPLICATION_JSON)
                .content(lote("2027-07", "2027-07-08", """
                        { "legajo": "u100", "metrica": "FCR", "valor": 70.5 },
                        { "legajo": "u100", "metrica": "TMO", "valor": 712, "fechaDeCorte": "2027-07-01" }
                        """)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cortes", org.hamcrest.Matchers.contains("2027-07-01", "2027-07-08")));
    }

    /** Sin valor no hay medición: una métrica que no llegó se deja faltante. */
    @Test
    void rechazaUnaMedicionSinValor() throws Exception {
        mvc().perform(post("/api/mediciones").contentType(MediaType.APPLICATION_JSON)
                .content(lote("2027-08", "2027-08-08", """
                        { "legajo": "u100", "metrica": "NPS", "valor": null }
                        """)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]", org.hamcrest.Matchers.containsString("valor")));
    }
}
