package com.lautaro.multimetrico.controller;

import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.CAMPANIA;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.mediciones;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.padron;
import static com.lautaro.multimetrico.fixture.EscenarioDeEjemplo.pliego;
import static com.lautaro.multimetrico.fixture.Identidades.admin;
import static com.lautaro.multimetrico.fixture.Identidades.asesor;
import static com.lautaro.multimetrico.fixture.Identidades.como;
import static com.lautaro.multimetrico.fixture.Identidades.supervisor;
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

/**
 * Quién ve qué.
 *
 * <p>El requisito del proyecto está escrito así: <b>un asesor que pide la fila de
 * otro recibe 403, nunca la fila</b>. "Nunca la fila" es lo que hace que estos
 * tests miren el <em>cuerpo</em> de la respuesta y no sólo el código: un 403 con
 * los datos adentro sería exactamente el error que se quiere evitar, y pasaría un
 * test que sólo comparara el status.
 *
 * <p>Período propio para no cruzarse con las otras clases, que comparten la base
 * H2. 2050 está libre: 2029 parecía estarlo y no lo estaba —lo usa
 * MultimetricoServiceTest— y el choque sólo aparece al correr la suite entera,
 * nunca al correr esta clase sola. Es la trampa que el handoff documenta.
 */
@SpringBootTest
class SeguridadTest {

    private static final YearMonth PERIODO = YearMonth.of(2050, 1);

    /** El líder que trae el escenario de ejemplo. */
    private static final String PERALTA = "PERALTA SILVIA";

    /** Dos asesores del equipo de PERALTA, según el escenario. */
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

    private String url(String legajo) {
        return "/api/multimetrico/%s/asesores/%s".formatted(PERIODO, legajo);
    }

    // ── sin token ────────────────────────────────────────────────────────────

    @Test
    void sinTokenNoSeVeNada() throws Exception {
        mvc().perform(get(url(FLAVIA)).param("campania", CAMPANIA))
                .andExpect(status().isUnauthorized())
                // El 401 sale con el mismo formato que el resto de la API, no con
                // el cuerpo vacío que devuelve Spring Security por defecto.
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void elLoginEsLoUnicoPublico() throws Exception {
        mvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"legajo\":\"nadie\",\"password\":\"x\"}"))
                // 401 y no 403: llega al endpoint, lo que falla son las credenciales.
                .andExpect(status().isUnauthorized());
    }

    // ── el control por fila ──────────────────────────────────────────────────

    @Test
    void unAsesorVeSuPropiaFila() throws Exception {
        mvc().perform(get(url(FLAVIA)).param("campania", CAMPANIA).with(como(asesor(FLAVIA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legajo").value(FLAVIA));
    }

    /** El requisito, textual: nunca la fila. */
    @Test
    void unAsesorQuePideLaFilaDeOtroRecibe403YNoLaFila() throws Exception {
        String cuerpo = mvc()
                .perform(get(url(OTRO)).param("campania", CAMPANIA).with(como(asesor(FLAVIA))))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Lo importante no es el 403 sino que no haya viajado nada de la fila.
        assertThat(cuerpo).doesNotContain("alcanceTotal");
        assertThat(cuerpo).doesNotContain("metricas");
        assertThat(cuerpo).contains("403");
    }

    @Test
    void unAsesorNoVeLaTablaDelEquipo() throws Exception {
        String cuerpo = mvc()
                .perform(get("/api/multimetrico/" + PERIODO).param("campania", CAMPANIA)
                        .with(como(asesor(FLAVIA))))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(cuerpo).doesNotContain("filas");
    }

    // ── supervisores ─────────────────────────────────────────────────────────

    @Test
    void unSupervisorVeSuEquipo() throws Exception {
        mvc().perform(get("/api/multimetrico/" + PERIODO)
                        .param("campania", CAMPANIA)
                        .with(como(supervisor(PERALTA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lider").value(PERALTA));
    }

    /**
     * Sin pedir equipo, un supervisor obtiene el suyo y no la operación entera.
     *
     * <p>Se acota en vez de rechazarse: el resultado es exactamente lo que ya
     * tenía derecho a ver, y un 403 acá sería pedirle que repita el dato que el
     * token ya trae.
     */
    @Test
    void unSupervisorSinPedirEquipoObtieneElSuyo() throws Exception {
        mvc().perform(get("/api/multimetrico/" + PERIODO)
                        .param("campania", CAMPANIA)
                        .with(como(supervisor(PERALTA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lider").value(PERALTA));
    }

    @Test
    void unSupervisorNoVeElEquipoDeOtro() throws Exception {
        mvc().perform(get("/api/multimetrico/" + PERIODO)
                        .param("campania", CAMPANIA)
                        .param("lider", "CABRERA MONICA")
                        .with(como(supervisor(PERALTA))))
                .andExpect(status().isForbidden());
    }

    // ── cargar datos es de admin ─────────────────────────────────────────────

    @Test
    void unSupervisorNoCargaPliegos() throws Exception {
        mvc().perform(post("/api/pliegos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodo\":\"2050-09\",\"campania\":\"X\",\"escalas\":[]}")
                        .with(como(supervisor(PERALTA))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unAdminVeCualquierFila() throws Exception {
        mvc().perform(get(url(OTRO)).param("campania", CAMPANIA).with(como(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legajo").value(OTRO));
    }

    // ── el login ─────────────────────────────────────────────────────────────

    @Test
    void elLoginDevuelveUnTokenQueSePuedeLeer() {
        usuarios.crear(new UsuarioRequest("k900", "Prueba Login", "unacontrasenalarga", Rol.ASESOR, null));

        var respuesta = usuarios.entrar(new LoginRequest("k900", "unacontrasenalarga"));

        assertThat(respuesta.legajo()).isEqualTo("k900");
        assertThat(respuesta.rol()).isEqualTo(Rol.ASESOR);

        assertThat(tokens.leer(respuesta.token())).hasValueSatisfying(credencial -> {
            assertThat(credencial.legajo()).isEqualTo("k900");
            assertThat(credencial.rol()).isEqualTo(Rol.ASESOR);
        });
    }

    /**
     * El legajo inexistente y la contraseña equivocada dan el <b>mismo</b>
     * mensaje.
     *
     * <p>Distinguirlos convertiría al login en una forma de averiguar qué legajos
     * existen, que es la mitad del trabajo de quien después prueba contraseñas.
     */
    @Test
    void elLoginNoDiceSiFalloElLegajoOLaContrasena() {
        usuarios.crear(new UsuarioRequest("k901", "Prueba Mensaje", "unacontrasenalarga", Rol.ASESOR, null));

        String porLegajo = mensajeDeFallo(new LoginRequest("no-existe-nadie-asi", "loquesea"));
        String porPassword = mensajeDeFallo(new LoginRequest("k901", "la-equivocada"));

        assertThat(porLegajo).isEqualTo(porPassword);
    }

    @Test
    void unTokenFalsificadoNoSirve() {
        assertThat(tokens.leer("no.es.un.token")).isEmpty();
        assertThat(tokens.leer("")).isEmpty();
    }

    /** Un SUPERVISOR sin equipo no vería a nadie: se rechaza al crearlo. */
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
