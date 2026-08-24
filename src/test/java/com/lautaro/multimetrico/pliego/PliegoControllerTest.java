package com.lautaro.multimetrico.pliego;

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

@SpringBootTest
class PliegoControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc;

    private MockMvc mvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        }
        return mockMvc;
    }

    private static final String PLIEGO_SANO = """
            {
              "periodo": "2026-08",
              "campania": "CONVERGENTES",
              "escalas": [{
                "metrica": "NPS",
                "peso": 3.5,
                "bandas": [
                  { "desde": null, "hasta": 50,   "resultado": 0 },
                  { "desde": 50,   "hasta": null, "resultado": 3.5 }
                ]
              }]
            }
            """;

    /** Una banda que se pisa con la siguiente no se guarda: 422 y el motivo. */
    private static final String PLIEGO_SUPERPUESTO = """
            {
              "periodo": "2026-09",
              "campania": "CONVERGENTES",
              "escalas": [{
                "metrica": "TMO",
                "peso": 3,
                "bandas": [
                  { "desde": null, "hasta": 681,  "resultado": 3 },
                  { "desde": 680,  "hasta": null, "resultado": 0 }
                ]
              }]
            }
            """;

    @Test
    void cargaUnPliegoValido() throws Exception {
        mvc().perform(post("/api/pliegos").contentType(MediaType.APPLICATION_JSON).content(PLIEGO_SANO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodo").value("2026-08"))
                .andExpect(jsonPath("$.alcanceMaximo").value(3.5))
                .andExpect(jsonPath("$.escalas[0].bandas[0].rango").value("[-∞, 50)"));
    }

    @Test
    void rechazaUnPliegoConBandasSuperpuestas() throws Exception {
        mvc().perform(post("/api/pliegos").contentType(MediaType.APPLICATION_JSON).content(PLIEGO_SUPERPUESTO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalles", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.detalles[0]", org.hamcrest.Matchers.containsString("se pisan")));
    }

    /** La revisión no guarda: sirve para mirar el pliego del mes antes de comprometerlo. */
    @Test
    void laRevisionDevuelveLosProblemasSinGuardar() throws Exception {
        mvc().perform(post("/api/pliegos/revision").contentType(MediaType.APPLICATION_JSON)
                        .content(PLIEGO_SUPERPUESTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));

        mvc().perform(get("/api/pliegos/2026-09").param("campania", "CONVERGENTES"))
                .andExpect(status().isNotFound());
    }

    @Test
    void evaluarDevuelveLaBandaJuntoConElResultado() throws Exception {
        mvc().perform(post("/api/pliegos").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "periodo": "2026-10",
                  "campania": "FU",
                  "escalas": [{
                    "metrica": "FCR",
                    "peso": 3.5,
                    "bandas": [
                      { "desde": null, "hasta": 80.1, "resultado": 0 },
                      { "desde": 80.1, "hasta": null, "resultado": 3.5 }
                    ]
                  }]
                }
                """))
                .andExpect(status().isCreated());

        mvc().perform(post("/api/pliegos/2026-10/evaluacion").param("campania", "FU")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "mediciones": { "FCR": 80.04 } }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alcanceTotal").value(0))
                .andExpect(jsonPath("$.alcances[0].banda").value("[-∞, 80.1)"))
                .andExpect(jsonPath("$.completa").value(true));
    }
}
