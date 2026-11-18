package com.lautaro.multimetrico.controller;

import static com.lautaro.multimetrico.fixture.Identidades.admin;
import static com.lautaro.multimetrico.fixture.Identidades.como;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class PerfilControllerTest {

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

    @Test
    void unPerfilSeDefineYApareceEnLaLista() throws Exception {
        mvc().perform(post("/api/perfiles").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "nombre": "nps-del-panel",
                          "metrica": "NPS",
                          "hoja": "Datos",
                          "filaEncabezados": 2,
                          "columnaAsesor": "ASESOR",
                          "columnaValor": "VALOR",
                          "separadorDecimal": "COMA",
                          "escala": "TAL_CUAL"
                        }
                        """).with(como(admin())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("nps-del-panel"))
                .andExpect(jsonPath("$.metrica").value("NPS"))
                .andExpect(jsonPath("$.filaEncabezados").value(2))
                .andExpect(jsonPath("$.formatoNumero.separadorDecimal").value("COMA"))
                .andExpect(jsonPath("$.formatoNumero.escala").value("TAL_CUAL"));

        mvc().perform(get("/api/perfiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("nps-del-panel")));
    }

    @Test
    void laInspeccionMuestraHojasYEjemplosDeCadaColumna() throws Exception {
        byte[] planilla = PlanillaSintetica.hoja("Datos")
                .fila("ASESOR", "VALOR", "LIDER")
                .filaConValor("SOSA ANA", 75.0, "LEDESMA JORGE")
                .bytes();

        mvc().perform(multipart("/api/perfiles/inspeccion")
                        .file(new MockMultipartFile("archivo", "muestra.xlsx", null, planilla))
                        .with(como(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hojas[0]").value("Datos"))
                .andExpect(jsonPath("$.filaEncabezados").value(1))
                .andExpect(jsonPath("$.columnas[0].encabezado").value("ASESOR"))
                .andExpect(jsonPath("$.columnas[0].ejemplos[0]").value("SOSA ANA"));
    }

    @Test
    void laInspeccionLeeLaHojaYLaFilaDeEncabezadosQueSeLePiden() throws Exception {
        byte[] planilla = PlanillaSintetica.hoja("Datos")
                .fila("REPORTE SEMANAL")
                .fila("ASESOR", "VALOR", "LIDER")
                .filaConValor("SOSA ANA", 75.0, "LEDESMA JORGE")
                .bytes();

        mvc().perform(multipart("/api/perfiles/inspeccion")
                        .file(new MockMultipartFile("archivo", "muestra.xlsx", null, planilla))
                        .param("hoja", "Datos")
                        .param("filaEncabezados", "2")
                        .with(como(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filaEncabezados").value(2))
                .andExpect(jsonPath("$.columnas[0].encabezado").value("ASESOR"))
                .andExpect(jsonPath("$.columnas[0].ejemplos[0]").value("SOSA ANA"));
    }
}
