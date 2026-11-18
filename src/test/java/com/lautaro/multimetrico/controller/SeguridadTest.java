package com.lautaro.multimetrico.controller;

import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.CAMPANIA;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.mediciones;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.padron;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.pliego;
import static com.lautaro.multimetrico.fixture.Identidades.admin;
import static com.lautaro.multimetrico.fixture.Identidades.asesor;
import static com.lautaro.multimetrico.fixture.Identidades.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lautaro.multimetrico.dto.seguridad.LoginRequest;
import com.lautaro.multimetrico.dto.seguridad.UsuarioRequest;
import com.lautaro.multimetrico.enumeration.Rol;
import com.lautaro.multimetrico.service.medicion.MedicionService;
import com.lautaro.multimetrico.service.padron.PadronService;
import com.lautaro.multimetrico.service.pliego.PliegoService;
import com.lautaro.multimetrico.service.seguridad.ServicioDeTokens;
import com.lautaro.multimetrico.service.seguridad.UsuarioService;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SeguridadTest {

    private static final YearMonth PERIODO = YearMonth.of(2050, 1);

    private static final String FLAVIA = "u100";
    private static final String OTRO = "u200";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private PliegoService pliegos;

    @Autowired
    private PadronService padrones;

    @Autowired
    private MedicionService medicionesDelMes;

    @Autowired
    private UsuarioService usuarios;

    @Autowired
    private ServicioDeTokens tokens;

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

    private String filaDe(String legajo) {
        return "/api/multimetrico/%s/asesores/%s".formatted(PERIODO, legajo);
    }


    @Test
    void laTablaDelEquipoSeVeSinEntrar() throws Exception {
        mvc().perform(get("/api/multimetrico/" + PERIODO).param("campania", CAMPANIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filas").isArray());
    }

    @Test
    void laFilaDeCualquieraSeVeSinEntrar() throws Exception {
        mvc().perform(get(filaDe(OTRO)).param("campania", CAMPANIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legajo").value(OTRO));
    }

    @Test
    void elPliegoSeVeSinEntrar() throws Exception {
        mvc().perform(get("/api/pliegos"))
                .andExpect(status().isOk());
    }

    @Test
    void entrarNoCambiaLoQueSeLee() throws Exception {
        mvc().perform(get(filaDe(OTRO)).param("campania", CAMPANIA).with(como(asesor(FLAVIA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legajo").value(OTRO));
    }


    @Test
    void cargarUnPliegoSinEntrarDa401() throws Exception {
        mvc().perform(post("/api/pliegos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodo\":\"2050-09\",\"campania\":\"X\",\"escalas\":[]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void cargarMedicionesSinEntrarDa401() throws Exception {
        mvc().perform(post("/api/mediciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodo\":\"2050-09\",\"fechaDeCorte\":\"2050-09-01\",\"mediciones\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unAsesorNoCargaPliegos() throws Exception {
        mvc().perform(post("/api/pliegos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodo\":\"2050-09\",\"campania\":\"X\",\"escalas\":[]}")
                        .with(como(asesor(FLAVIA))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarUsuariosNoEsPublicoAunqueSeaUnGet() throws Exception {
        mvc().perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());

        mvc().perform(get("/api/usuarios").with(como(admin())))
                .andExpect(status().isOk());
    }


    @Test
    void aUnGetPublicoLeFaltaUnParametroYDice400() throws Exception {
        mvc().perform(get("/api/multimetrico/" + PERIODO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("campania")));
    }

    @Test
    void aUnPostAutenticadoLeFaltaElArchivoYDice400() throws Exception {
        mvc().perform(post("/api/importaciones").with(como(admin())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unPeriodoConFormatoInvalidoDice400() throws Exception {
        mvc().perform(get("/api/multimetrico/agosto").param("campania", CAMPANIA))
                .andExpect(status().isBadRequest());
    }


    @Test
    void elLoginDevuelveUnTokenQueSePuedeLeer() {
        usuarios.crear(new UsuarioRequest("k900", "Prueba Login", "unacontrasenalarga", Rol.ADMIN, null));

        var respuesta = usuarios.entrar(new LoginRequest("k900", "unacontrasenalarga"));

        assertThat(respuesta.legajo()).isEqualTo("k900");
        assertThat(respuesta.rol()).isEqualTo(Rol.ADMIN);

        assertThat(tokens.leer(respuesta.token())).hasValueSatisfying(credencial -> {
            assertThat(credencial.legajo()).isEqualTo("k900");
            assertThat(credencial.rol()).isEqualTo(Rol.ADMIN);
        });
    }

    @Test
    void unLoginQueNoCoincideDa401() throws Exception {
        mvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"legajo\":\"nadie\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elLoginNoDiceSiFalloElLegajoOLaContrasena() {
        usuarios.crear(new UsuarioRequest("k901", "Prueba Mensaje", "unacontrasenalarga", Rol.ADMIN, null));

        String porLegajo = mensajeDeFallo(new LoginRequest("no-existe-nadie-asi", "loquesea"));
        String porPassword = mensajeDeFallo(new LoginRequest("k901", "la-equivocada"));

        assertThat(porLegajo).isEqualTo(porPassword);
    }

    @Test
    void unTokenFalsificadoNoSirve() {
        assertThat(tokens.leer("no.es.un.token")).isEmpty();
        assertThat(tokens.leer("")).isEmpty();
    }

    @Test
    void unSupervisorNecesitaEquipo() {
        assertThat(
                org.junit.jupiter.api.Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> usuarios.crear(new UsuarioRequest(
                                "k902", "Sin Equipo", "unacontrasenalarga", Rol.SUPERVISOR, null)))
                        .getMessage())
                .contains("equipo");
    }

    private String mensajeDeFallo(LoginRequest request) {
        return org.junit.jupiter.api.Assertions.assertThrows(
                com.lautaro.multimetrico.exception.CredencialesInvalidasException.class,
                () -> usuarios.entrar(request))
                .getMessage();
    }
}
