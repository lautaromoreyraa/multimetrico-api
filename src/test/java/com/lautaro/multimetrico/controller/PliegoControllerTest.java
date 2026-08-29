package com.lautaro.multimetrico.controller;

import static com.lautaro.multimetrico.fixture.Identidades.admin;
import static com.lautaro.multimetrico.fixture.Identidades.como;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * El pliego por HTTP.
 *
 * <p>Estas aserciones son el contrato que consume el frontend, así que valen el
 * doble después de la reestructuración: si una se movió, se movió la API.
 */
@SpringBootTest
class PliegoControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc;

    private MockMvc mvc() {
        if (mockMvc == null) {
            // springSecurity() mete la cadena de seguridad real en el MockMvc. Sin
            // esto los tests pasan por al lado de toda la autorizacion: un GET sin
            // token devolveria 200 y nadie se enteraria de que la regla no anda.
            mockMvc = MockMvcBuilders.webAppContextSetup(contexto)
                    .apply(SecurityMockMvcConfigurers.springSecurity())
                    .build();
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
        mvc().perform(post("/api/pliegos").contentType(MediaType.APPLICATION_JSON).content(PLIEGO_SANO)
                        .with(como(admin())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodo").value("2026-08"))
                .andExpect(jsonPath("$.alcanceMaximo").value(3.5))
                .andExpect(jsonPath("$.escalas[0].bandas[0].rango").value("[-∞, 50)"))
                // La unidad la sabe el enum Metrica y viaja en la escala, para que la
                // pantalla no tenga que deducirla del nombre de la métrica.
                .andExpect(jsonPath("$.escalas[0].unidad").value("PORCENTAJE"));
    }

    @Test
    void rechazaUnPliegoConBandasSuperpuestas() throws Exception {
        mvc().perform(post("/api/pliegos").contentType(MediaType.APPLICATION_JSON).content(PLIEGO_SUPERPUESTO)
                        .with(como(admin())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalles", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.detalles[0]", org.hamcrest.Matchers.containsString("se pisan")));
    }

    /** La revisión no guarda: sirve para mirar el pliego del mes antes de comprometerlo. */
    @Test
    void laRevisionDevuelveLosProblemasSinGuardar() throws Exception {
        mvc().perform(post("/api/pliegos/revision").contentType(MediaType.APPLICATION_JSON)
                        .content(PLIEGO_SUPERPUESTO)
                        .with(como(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));

        mvc().perform(get("/api/pliegos/2026-09").param("campania", "CONVERGENTES")
                        .with(como(admin())))
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
                """)
                        .with(como(admin())))
                .andExpect(status().isCreated());

        mvc().perform(post("/api/pliegos/2026-10/evaluacion").param("campania", "FU")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "mediciones": { "FCR": 80.04 } }
                                """)
                        .with(como(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alcanceTotal").value(0))
                .andExpect(jsonPath("$.alcances[0].banda").value("[-∞, 80.1)"))
                .andExpect(jsonPath("$.completa").value(true));
    }

    /**
     * Volver a cargar el mismo pliego devuelve 409 y no lo pisa.
     *
     * <p>No estaba cubierto por HTTP y es la regla central del proyecto: si el
     * pliego de un mes se puede sobrescribir, el cálculo de ese mes deja de ser
     * reproducible.
     *
     * <p>Usa un período <b>propio</b> y no el de {@link #cargaUnPliegoValido}: la
     * base se comparte entre métodos y JUnit no garantiza el orden, así que
     * apoyarse en el pliego del otro test hacía que, según cuál corriera primero,
     * el que esperaba un 201 recibiera este 409.
     */
    @Test
    void unPliegoYaCargadoNoSePisa() throws Exception {
        String pliego = PLIEGO_SANO.replace("2026-08", "2026-11");

        mvc().perform(post("/api/pliegos").contentType(MediaType.APPLICATION_JSON).content(pliego)
                        .with(como(admin())))
                .andExpect(status().isCreated());

        mvc().perform(post("/api/pliegos").contentType(MediaType.APPLICATION_JSON).content(pliego)
                        .with(como(admin())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("Ya hay un pliego")));
    }
}
