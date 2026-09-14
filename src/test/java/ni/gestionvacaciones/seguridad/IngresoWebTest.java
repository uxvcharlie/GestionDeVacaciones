package ni.gestionvacaciones.seguridad;

import ni.gestionvacaciones.PruebaConPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El ingreso real por el formulario: contraseña, bloqueo por intentos y usuarios bloqueados. */
class IngresoWebTest extends PruebaConPostgres {

    private static final String CLAVE = "frase-correcta-del-ingreso";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private ServicioUsuario servicioUsuario;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Cinco contraseñas equivocadas bloquean; ni la correcta entra hasta desbloquear")
    void cincoIntentosBloquean() throws Exception {
        Usuario usuario = crear("ingreso-web-bloqueo");

        for (int i = 0; i < 4; i++) {
            mvc.perform(ingreso("ingreso-web-bloqueo", "contraseña-equivocada"))
                    .andExpect(redirectedUrl("/ingresar?error"));
        }
        mvc.perform(ingreso("ingreso-web-bloqueo", "contraseña-equivocada"))
                .andExpect(redirectedUrl("/ingresar?bloqueado"));
        mvc.perform(ingreso("ingreso-web-bloqueo", CLAVE))
                .andExpect(redirectedUrl("/ingresar?bloqueado"));

        String pantalla = mvc.perform(get("/ingresar").param("bloqueado", ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(pantalla).contains("quedó bloqueado 15 minutos después de 5 intentos fallidos");

        usuario = usuarioRepositorio.findById(usuario.getId()).orElseThrow();
        usuario.setBloqueadoHasta(null);
        usuarioRepositorio.save(usuario);

        mvc.perform(ingreso("ingreso-web-bloqueo", CLAVE)).andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("Un ingreso correcto lleva al inicio y anota el último ingreso")
    void ingresoCorrecto() throws Exception {
        Usuario usuario = crear("ingreso-web-correcto");

        mvc.perform(ingreso("Ingreso-Web-Correcto", CLAVE)).andExpect(redirectedUrl("/"));

        assertThat(usuarioRepositorio.findById(usuario.getId()).orElseThrow().getUltimoLogin()).isNotNull();
    }

    @Test
    @DisplayName("Un usuario inexistente recibe el mismo mensaje que una contraseña equivocada")
    void usuarioInexistente() throws Exception {
        mvc.perform(ingreso("usuario-que-no-existe", CLAVE)).andExpect(redirectedUrl("/ingresar?error"));
    }

    @Test
    @DisplayName("Un usuario bloqueado por administración no entra, y ve un mensaje claro")
    void bloqueadoPorAdministracion() throws Exception {
        Usuario usuario = crear("ingreso-web-desactivado");
        usuario.setActivo(false);
        usuarioRepositorio.save(usuario);

        mvc.perform(ingreso("ingreso-web-desactivado", CLAVE)).andExpect(redirectedUrl("/ingresar?desactivado"));

        String pantalla = mvc.perform(get("/ingresar").param("desactivado", ""))
                .andReturn().getResponse().getContentAsString();
        assertThat(pantalla).contains("Tu usuario está bloqueado");
    }

    @Test
    @DisplayName("Si lo bloquean con la sesión abierta, se le cierra en el siguiente clic")
    void sesionAbiertaSeCierra() throws Exception {
        Usuario usuario = crear("ingreso-web-sesion-abierta");
        usuario.setActivo(false);
        usuarioRepositorio.save(usuario);

        mvc.perform(get("/").with(user("ingreso-web-sesion-abierta").roles("ADMIN")))
                .andExpect(redirectedUrl("/ingresar?desactivado"));
    }

    private Usuario crear(String username) {
        Usuario usuario = servicioUsuario.crear(username, CLAVE, "Persona de Ingreso", null);
        usuario.setDebeCambiarPassword(false);
        return usuarioRepositorio.save(usuario);
    }

    private static org.springframework.test.web.servlet.RequestBuilder ingreso(String usuario, String clave) {
        return formLogin("/ingresar").userParameter("usuario").passwordParam("clave").user(usuario).password(clave);
    }
}
