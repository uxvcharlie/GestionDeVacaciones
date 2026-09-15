package ni.gestionvacaciones.admin;

import ni.gestionvacaciones.PruebaConPostgres;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.seguridad.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Panel de usuarios, bitácora y parámetros, de punta a punta por HTTP. */
class AdminWebTest extends PruebaConPostgres {

    private static final String USUARIO_PRUEBA = "prueba-admin";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private ServicioUsuario servicioUsuario;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private ServicioParametros parametros;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        if (!usuarioRepositorio.existsByUsername(USUARIO_PRUEBA)) {
            Usuario usuario = servicioUsuario.crear(USUARIO_PRUEBA, "frase-larga-del-administrador", "Admin de Pruebas", null);
            usuario.setDebeCambiarPassword(false);
            usuarioRepositorio.save(usuario);
        }
    }

    @Test
    @DisplayName("La lista de usuarios marca el propio y ofrece crear")
    void listaDeUsuarios() throws Exception {
        assertThat(html(get("/admin/usuarios")))
                .contains("Administración", "Usuarios", "Crear usuario", "Este es tu usuario.", "Bitácora", "Parámetros");
    }

    @Test
    @DisplayName("Crear un usuario muestra la contraseña temporal una sola vez")
    void crearMuestraLaPasswordUnaVez() throws Exception {
        MvcResult alta = mvc.perform(post("/admin/usuarios").with(sesion()).with(csrf())
                        .param("nombreCompleto", "Web Persona Nueva")
                        .param("username", "web.persona.nueva"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String entrega = alta.getResponse().getRedirectedUrl();
        assertThat(entrega).matches("/admin/usuarios/\\d+/entregar");
        String temporal = (String) alta.getFlashMap().get("passwordTemporal");

        assertThat(html(get(entrega).flashAttrs(alta.getFlashMap())))
                .contains("web.persona.nueva", temporal, "no se vuelve a mostrar");

        mvc.perform(get(entrega).with(sesion())).andExpect(redirectedUrl("/admin/usuarios"));
    }

    @Test
    @DisplayName("Un nombre de usuario con espacios se rechaza con un mensaje claro")
    void usernameInvalido() throws Exception {
        assertThat(html(post("/admin/usuarios").with(csrf())
                .param("nombreCompleto", "Alguien")
                .param("username", "con espacios")))
                .contains("Sin espacios.");
    }

    @Test
    @DisplayName("La bitácora muestra las acciones y se puede filtrar")
    void bitacora() throws Exception {
        mvc.perform(post("/admin/usuarios").with(sesion()).with(csrf())
                .param("nombreCompleto", "Web Persona Bitácora")
                .param("username", "web.persona.bitacora"));

        assertThat(html(get("/admin/bitacora").param("accion", "USUARIO_CREADO")))
                .contains("Creó un usuario", "Web Persona Bitácora", "Por Admin de Pruebas");
        assertThat(html(get("/admin/bitacora").param("accion", "PARAMETRO_CAMBIADO")))
                .doesNotContain("Web Persona Bitácora");
    }

    @Test
    @DisplayName("Cambiar la jornada muestra un ejemplo y pide confirmación antes de guardar")
    void parametrosConConfirmacion() throws Exception {
        try {
            String confirmacion = html(post("/admin/parametros").with(csrf())
                    .param("horasPorJornada", "6").param("zonaHoraria", "America/Managua"));
            assertThat(confirmacion).contains("¿Cambiar la jornada de 8 a 6 horas?", "10 días", "13 días y 2 horas");
            assertThat(parametros.horasPorJornada()).as("todavía no se guardó").isEqualTo(8);

            mvc.perform(post("/admin/parametros").with(sesion()).with(csrf())
                            .param("horasPorJornada", "6").param("zonaHoraria", "America/Managua")
                            .param("confirmado", "true"))
                    .andExpect(redirectedUrl("/admin/parametros"));
            assertThat(parametros.horasPorJornada()).isEqualTo(6);

            assertThat(html(get("/admin/bitacora").param("accion", "PARAMETRO_CAMBIADO")))
                    .contains("horas_por_jornada: 8 → 6");
        } finally {
            parametros.actualizar(8, "America/Managua", null, null);
        }
    }

    @Test
    @DisplayName("Una zona horaria que no existe se rechaza sin guardar")
    void zonaInvalida() throws Exception {
        assertThat(html(post("/admin/parametros").with(csrf())
                .param("horasPorJornada", "8").param("zonaHoraria", "Marte/Olimpo")))
                .contains("no existe");
        assertThat(parametros.zonaHoraria().getId()).isEqualTo("America/Managua");
    }

    @Test
    @DisplayName("Sin el rol ADMIN no se entra a administración")
    void sinRolAdmin() throws Exception {
        mvc.perform(get("/admin/usuarios").with(user("alguien-sin-rol").roles("OTRO")))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor sesion() {
        return user(USUARIO_PRUEBA).roles("ADMIN");
    }

    private String html(MockHttpServletRequestBuilder peticion) throws Exception {
        return mvc.perform(peticion.with(sesion()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
