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

/**
 * Qué se puede hacer sin entrar y qué no.
 *
 * <p>La regla está en el verbo, no en el recurso: <b>leer es público, escribir es
 * de administrador</b>. Estos tests existen para que esa línea no se corra sola
 * —un endpoint de escritura nuevo que quede abierto, o un GET que empiece a pedir
 * token y rompa la pantalla— porque ninguna de las dos cosas se nota mirando.
 *
 * <p>Período propio para no cruzarse con las otras clases, que comparten la base
 * H2. 2050 está libre: 2029 parecía estarlo y no lo estaba —lo usa
 * MultimetricoServiceTest— y el choque sólo aparece al correr la suite entera,
 * nunca al correr esta clase sola. Es la trampa que el handoff documenta.
 */
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

    // ── leer es público ──────────────────────────────────────────────────────

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

    /**
     * Un token de asesor no <em>reduce</em> lo que se ve.
     *
     * <p>Suena obvio y no lo es: mientras existió el control por fila, entrar
     * como asesor mostraba menos que no entrar. Que hoy den lo mismo es la
     * consecuencia de haber abierto la lectura, y este test lo deja escrito.
     */
    @Test
    void entrarNoCambiaLoQueSeLee() throws Exception {
        mvc().perform(get(filaDe(OTRO)).param("campania", CAMPANIA).with(como(asesor(FLAVIA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legajo").value(OTRO));
    }

    // ── escribir pide administrador ──────────────────────────────────────────

    @Test
    void cargarUnPliegoSinEntrarDa401() throws Exception {
        mvc().perform(post("/api/pliegos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodo\":\"2050-09\",\"campania\":\"X\",\"escalas\":[]}"))
                .andExpect(status().isUnauthorized())
                // El 401 sale con el mismo formato que el resto de la API, no con
                // el cuerpo vacío que devuelve Spring Security por defecto.
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

    /** Autenticado pero sin ser administrador: 403 y no 401. */
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
        // La excepción a "todo GET es público": leer quién tiene acceso es
        // distinto de leer el multimétrico.
        mvc().perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());

        mvc().perform(get("/api/usuarios").with(como(admin())))
                .andExpect(status().isOk());
    }

    // ── el login ─────────────────────────────────────────────────────────────

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
                // 401 y no 403: llega al endpoint, lo que falla son las credenciales.
                .andExpect(status().isUnauthorized());
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
