package com.lautaro.multimetrico.controller;

import static com.lautaro.multimetrico.fixture.Identidades.admin;
import static com.lautaro.multimetrico.fixture.Identidades.como;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class PadronControllerTest {

    private static final AtomicInteger MES = new AtomicInteger();

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc;
    private String periodo;

    private MockMvc mvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(contexto)
                    .apply(SecurityMockMvcConfigurers.springSecurity())
                    .build();
        }
        return mockMvc;
    }

    private ResultActions enviar(String metodo, String ruta, String cuerpo) throws Exception {
        var pedido = switch (metodo) {
            case "POST" -> post(ruta);
            case "PUT" -> put(ruta);
            default -> throw new IllegalArgumentException(metodo);
        };
        return mvc().perform(pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo).with(como(admin())));
    }

    @BeforeEach
    void cargarPadron() throws Exception {
        periodo = "2061-%02d".formatted(MES.incrementAndGet());
        enviar("POST", "/api/padrones", """
                {
                  "periodo": "%s",
                  "asesores": [
                    { "legajo": "u100", "nombre": "AGUIRRE FLAVIA YAMILA", "lider": "PERALTA SILVIA" },
                    { "legajo": "u200", "nombre": "MIÑO HECTOR BAUTISTA", "lider": "PERALTA SILVIA" },
                    { "legajo": "u300", "nombre": "SOSA ANA", "lider": "LEDESMA JORGE" }
                  ]
                }
                """.formatted(periodo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodo").value(periodo))
                .andExpect(jsonPath("$.asesores").value(3))
                .andExpect(jsonPath("$.lideres", hasSize(2)));
    }

    @Test
    void elPadronSeVeEnteroOFiltradoPorLider() throws Exception {
        mvc().perform(get("/api/padrones/" + periodo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        mvc().perform(get("/api/padrones/" + periodo).param("lider", "LEDESMA JORGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].legajo").value("u300"))
                .andExpect(jsonPath("$[0].nombre").value("SOSA ANA"))
                .andExpect(jsonPath("$[0].lider").value("LEDESMA JORGE"));
    }

    @Test
    void unAsesorSeAgregaSeCorrigeYSeQuita() throws Exception {
        String asesores = "/api/padrones/%s/asesores".formatted(periodo);

        enviar("POST", asesores, """
                { "legajo": "u400", "nombre": "ROMERO LUCIA", "lider": "LEDESMA JORGE" }
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.legajo").value("u400"))
                .andExpect(jsonPath("$.lider").value("LEDESMA JORGE"));

        enviar("PUT", asesores + "/u400", """
                { "nombre": "ROMERO LUCIA BELEN", "lider": "PERALTA SILVIA" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("ROMERO LUCIA BELEN"))
                .andExpect(jsonPath("$.lider").value("PERALTA SILVIA"));

        mvc().perform(delete(asesores + "/u400").with(como(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legajo").value("u400"));

        mvc().perform(get("/api/padrones/" + periodo))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void laResolucionSeparaLoResueltoDeLoPendiente() throws Exception {
        enviar("POST", "/api/padrones/%s/resolucion".formatted(periodo), """
                { "nombres": ["AGUIRRE FLAVIA YAMILA", "NADIE QUE EXISTA"] }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.resueltos").value(1))
                .andExpect(jsonPath("$.pendientes").value(1))
                .andExpect(jsonPath("$.completa").value(false))
                .andExpect(jsonPath("$.coincidencias[0].legajo").value("u100"))
                .andExpect(jsonPath("$.sinResolver[0].texto").value("NADIE QUE EXISTA"));
    }

    @Test
    void elAliasConfirmadoVuelveConTodosLosAliasDelLegajo() throws Exception {
        enviar("POST", "/api/padrones/%s/alias".formatted(periodo), """
                { "texto": "FLAVITA DEL PANEL", "legajo": "u100" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("FLAVITA DEL PANEL"))
                .andExpect(jsonPath("$.legajo").value("u100"))
                .andExpect(jsonPath("$.nombre").value("AGUIRRE FLAVIA YAMILA"))
                .andExpect(jsonPath("$.aliasDeEsteLegajo", hasItem("FLAVITA DEL PANEL")));
    }

    @Test
    void quitarUnLegajoQueNoEstaDa404() throws Exception {
        mvc().perform(delete("/api/padrones/%s/asesores/z999".formatted(periodo)).with(como(admin())))
                .andExpect(status().isNotFound());
    }
}
