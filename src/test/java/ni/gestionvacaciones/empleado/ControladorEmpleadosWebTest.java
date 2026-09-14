package ni.gestionvacaciones.empleado;

import ni.gestionvacaciones.PruebaConPostgres;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.seguridad.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba las pantallas de empleados de punta a punta: petición HTTP →
 * seguridad → controlador → base de datos → plantilla HTML.
 *
 * <p>Usa un usuario propio de pruebas que ya cambió su contraseña, para no
 * depender del administrador inicial (que sigue obligado a cambiarla).</p>
 */
class ControladorEmpleadosWebTest extends PruebaConPostgres {

    private static final String USUARIO_PRUEBA = "prueba-web";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private ServicioUsuario servicioUsuario;

    @Autowired
    private ServicioEmpleados servicioEmpleados;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();

        if (!usuarioRepositorio.existsByUsername(USUARIO_PRUEBA)) {
            Usuario usuario = servicioUsuario.crear(USUARIO_PRUEBA, "frase-larga-para-web", "Usuaria de Pruebas", null);
            usuario.setDebeCambiarPassword(false);
            usuarioRepositorio.save(usuario);
        }
    }

    private static RequestPostProcessor conSesion() {
        return user(USUARIO_PRUEBA).roles("ADMIN");
    }

    @Test
    @DisplayName("Sin sesión, /empleados manda a la pantalla de ingreso")
    void sinSesionPideIngresar() throws Exception {
        mvc.perform(get("/empleados")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Agregar por formulario lleva a la ficha con el saldo en lenguaje natural")
    void agregarPorFormulario() throws Exception {
        MvcResult alta = mvc.perform(post("/empleados").with(conSesion()).with(csrf())
                        .param("nombreCompleto", "Web Prueba Formulario")
                        .param("cargo", "Asistente")
                        .param("saldoDias", "10")
                        .param("saldoHoras", "0"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String ficha = alta.getResponse().getRedirectedUrl();
        assertThat(ficha).matches("/empleados/\\d+");

        String html = mvc.perform(get(ficha).with(conSesion()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Web Prueba Formulario", "10 días", "Saldo inicial", "Dar de baja");
        assertThat(html).doesNotContain("10.0", "10,0");
    }

    @Test
    @DisplayName("Los errores de validación se muestran en español humano")
    void validacionEnEspanol() throws Exception {
        String html = mvc.perform(post("/empleados").with(conSesion()).with(csrf())
                        .param("nombreCompleto", "   ")
                        .param("saldoDias", "-3")
                        .param("saldoHoras", "0"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Escribí el nombre completo.", "Los días no pueden ser negativos.");
        assertThat(html).doesNotContainIgnoringCase("constraint").doesNotContainIgnoringCase("null");
    }

    @Test
    @DisplayName("Un formulario sin token CSRF, o con uno falso, no se procesa")
    void sinCsrfNoSeProcesa() throws Exception {
        // Sin token y sin sesión: Spring Security lo trata como una sesión vencida
        // y manda a la pantalla de ingreso con aviso, en vez de un error seco.
        String destino = mvc.perform(post("/empleados").with(conSesion())
                        .param("nombreCompleto", "Intento Sin Token")
                        .param("saldoDias", "1")
                        .param("saldoHoras", "0"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(destino).startsWith("/ingresar");

        // Con un token inventado: 403, prohibido.
        mvc.perform(post("/empleados").with(conSesion()).with(csrf().useInvalidToken())
                        .param("nombreCompleto", "Intento Sin Token")
                        .param("saldoDias", "1")
                        .param("saldoHoras", "0"))
                .andExpect(status().isForbidden());

        // Y en ninguno de los dos casos se guardó nada.
        assertThat(servicioEmpleados.buscar("Intento Sin Token", true)).isEmpty();
    }

    @Test
    @DisplayName("Un empleado que no existe da 404, no un error técnico")
    void empleadoInexistente() throws Exception {
        mvc.perform(get("/empleados/99999999").with(conSesion())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("La baja pide confirmación con el nombre y después muestra «De baja»")
    void bajaConConfirmacion() throws Exception {
        String ficha = mvc.perform(post("/empleados").with(conSesion()).with(csrf())
                        .param("nombreCompleto", "Web Prueba Baja")
                        .param("saldoDias", "2")
                        .param("saldoHoras", "0"))
                .andReturn().getResponse().getRedirectedUrl();

        String confirmacion = mvc.perform(get(ficha + "/baja").with(conSesion()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(confirmacion).contains("¿Dar de baja a", "Web Prueba Baja", "Su historial se conserva.");

        mvc.perform(post(ficha + "/baja").with(conSesion()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        String despues = mvc.perform(get(ficha).with(conSesion()))
                .andReturn().getResponse().getContentAsString();
        assertThat(despues).contains("De baja", "Su historial se conserva.", "2 días");
    }

    @Test
    @DisplayName("El buscador encuentra sin tildes y el listado usa tarjetas")
    void buscador() throws Exception {
        mvc.perform(post("/empleados").with(conSesion()).with(csrf())
                .param("nombreCompleto", "Web Ramón Suárez")
                .param("saldoDias", "1")
                .param("saldoHoras", "4"));

        String html = mvc.perform(get("/empleados").param("q", "ramon suarez").with(conSesion()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Web Ramón Suárez", "1 día y 4 horas", "id=\"resultados\"");
        assertThat(html).doesNotContain("<table");
    }
}
