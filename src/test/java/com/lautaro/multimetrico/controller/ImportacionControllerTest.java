package com.lautaro.multimetrico.controller;

import static com.lautaro.multimetrico.fixture.Identidades.admin;
import static com.lautaro.multimetrico.fixture.Identidades.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lautaro.multimetrico.fixture.PlanillaSintetica;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ImportacionControllerTest {

    private static final String PERIODO = "2062-01";

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc;

    private MockMvc mvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(contexto)
                    .apply(SecurityMockMvcConfigurers.springSecurity())
                    .build();
        }
        return mockMvc;
    }

    private void prepararPadronYPerfil() throws Exception {
        mvc().perform(post("/api/padrones").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "periodo": "%s",
                          "asesores": [
                            { "legajo": "u100", "nombre": "AGUIRRE FLAVIA YAMILA", "lider": "PERALTA SILVIA" },
                            { "legajo": "u300", "nombre": "SOSA ANA", "lider": "LEDESMA JORGE" }
                          ]
                        }
                        """.formatted(PERIODO)).with(como(admin())))
                .andExpect(status().isCreated());

        mvc().perform(post("/api/perfiles").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "nombre": "fcr-del-controller",
                          "metrica": "FCR",
                          "hoja": "Datos",
                          "filaEncabezados": 1,
                          "columnaAsesor": "ASESOR",
                          "columnaValor": "VALOR",
                          "columnaLider": "LIDER",
                          "separadorDecimal": "COMA",
                          "escala": "TAL_CUAL"
                        }
                        """).with(como(admin())))
                .andExpect(status().isCreated());
    }

    private String esperarQueTermine(String id) throws Exception {
        for (int intento = 0; intento < 100; intento++) {
            String cuerpo = mvc().perform(get("/api/importaciones/" + id))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            if (!"EN_CURSO".equals(JsonPath.read(cuerpo, "$.estado"))) {
                return cuerpo;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("La importación %s no terminó a tiempo".formatted(id));
    }

    @Test
    void unaImportacionSeRecibeSeConsultaYSeLeResuelveUnPendiente() throws Exception {
        prepararPadronYPerfil();
        byte[] planilla = PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "VALOR", "LIDER")
                .filaConValor("AGUIRRE FLAVIA YAMILA", 82.4, "PERALTA SILVIA")
                .filaConValor("PEREZ QUIENSABE", 60.0, "LEDESMA JORGE")
                .bytes();

        String recibida = mvc().perform(multipart("/api/importaciones")
                        .file(new MockMultipartFile("archivo", "fcr.xlsx", null, planilla))
                        .param("perfil", "fcr-del-controller")
                        .param("periodo", PERIODO)
                        .param("corte", "2062-01-20")
                        .with(como(admin())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.perfil").value("fcr-del-controller"))
                .andExpect(jsonPath("$.metrica").value("FCR"))
                .andExpect(jsonPath("$.periodo").value(PERIODO))
                .andExpect(jsonPath("$.fechaDeCorte").value("2062-01-20"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(recibida, "$.id");

        String terminada = esperarQueTermine(id);
        assertThat((Integer) JsonPath.read(terminada, "$.importadas")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(terminada, "$.pendientes")).isEqualTo(1);

        mvc().perform(post("/api/importaciones/%s/pendientes/3".formatted(id)).param("legajo", "u300")
                        .with(como(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.importadas").value(2))
                .andExpect(jsonPath("$.pendientes").value(0));

        mvc().perform(get("/api/importaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)));
    }

    @Test
    void unaImportacionQueNoExisteDa404() throws Exception {
        mvc().perform(get("/api/importaciones/no-existe"))
                .andExpect(status().isNotFound());
    }
}
