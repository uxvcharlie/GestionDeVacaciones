package ni.gestionvacaciones.web;

import ni.gestionvacaciones.PruebaConPostgres;
import ni.gestionvacaciones.empleado.Empleado;
import ni.gestionvacaciones.empleado.FormularioEmpleado;
import ni.gestionvacaciones.empleado.ServicioEmpleados;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.seguridad.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tablero, buscador, aviso de sesión, errores amables y reactivación, por HTTP. */
class TableroWebTest extends PruebaConPostgres {

    private static final String USUARIO_PRUEBA = "prueba-tablero";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private ServicioUsuario servicioUsuario;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private ServicioEmpleados servicioEmpleados;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        if (!usuarioRepositorio.existsByUsername(USUARIO_PRUEBA)) {
            Usuario usuario = servicioUsuario.crear(USUARIO_PRUEBA, "frase-larga-para-el-tablero", "Tablero de Pruebas", null);
            usuario.setDebeCambiarPassword(false);
            usuarioRepositorio.save(usuario);
        }
    }

    @Test
    @DisplayName("El inicio saluda por el nombre y muestra resumen, últimas solicitudes y el aviso de sesión")
    void inicio() throws Exception {
        crearFuncionario("Tablero Inicio Con Datos", 2);

        String html = html(get("/"));
        assertThat(Pattern.compile("(Buenos días|Buenas tardes|Buenas noches), Tablero").matcher(html).find())
                .as("saludo con el primer nombre").isTrue();
        assertThat(html).contains("Registrar solicitud", "Resumen de", "Funcionarios activos",
                "Últimas solicitudes", "Buscar funcionario",
                "id=\"aviso-sesion\"", "name=\"sesion-minutos\" content=\"30\"");
    }

    @Test
    @DisplayName("El buscador del inicio devuelve solo los resultados, sin tildes, y una ayuda si está vacío")
    void buscador() throws Exception {
        crearFuncionario("Tablero Buscado Ñandú", 3);

        String resultados = html(get("/tablero/buscar").param("q", "nandu"));
        assertThat(resultados).contains("Tablero Buscado Ñandú", "1 funcionario", "3 días");
        assertThat(resultados).doesNotContain("<html", "<head");

        assertThat(html(get("/tablero/buscar").param("q", ""))).contains("Escribí un nombre");
    }

    @Test
    @DisplayName("«Seguir trabajando» renueva la sesión; sin sesión, no")
    void mantenerSesion() throws Exception {
        mvc.perform(get("/sesion/mantener").with(sesion())).andExpect(status().isNoContent());
        mvc.perform(get("/sesion/mantener")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Un formulario vencido avisa en el inicio en vez de mostrar un error")
    void formularioVencido() throws Exception {
        assertThat(html(get("/").param("vencido", ""))).contains("ya no era válido y no se guardó nada");
    }

    @Test
    @DisplayName("Abrir directo una dirección de formulario lleva al comienzo, no a un error")
    void direccionesDeFormulario() throws Exception {
        mvc.perform(get("/solicitudes/confirmar").with(sesion()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/solicitudes/nueva"));
        mvc.perform(get("/solicitudes").with(sesion()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/solicitudes/nueva"));
    }

    @Test
    @DisplayName("Reactivar desde la ficha: aparece el botón al dar de baja y deshace la baja")
    void reactivarDesdeLaFicha() throws Exception {
        Empleado funcionario = crearFuncionario("Tablero Reactivación Web", 4);
        String ficha = "/empleados/" + funcionario.getId();

        mvc.perform(post(ficha + "/baja").with(sesion()).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(html(get(ficha))).contains("Reactivar", "Si se dio de baja por error");

        mvc.perform(post(ficha + "/reactivar").with(sesion()).with(csrf())).andExpect(status().is3xxRedirection());
        String despues = html(get(ficha));
        assertThat(despues).contains("Dar de baja", "Registrar solicitud", "4 días");
        assertThat(despues).doesNotContain("Si se dio de baja por error");
    }

    // ---------------------------------------------------------------------

    private RequestPostProcessor sesion() {
        return user(USUARIO_PRUEBA).roles("ADMIN");
    }

    private String html(MockHttpServletRequestBuilder peticion) throws Exception {
        return mvc.perform(peticion.with(sesion()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private Empleado crearFuncionario(String nombre, int dias) {
        FormularioEmpleado formulario = new FormularioEmpleado();
        formulario.setNombreCompleto(nombre);
        formulario.setSaldoDias(dias);
        formulario.setSaldoHoras(0);
        return servicioEmpleados.crear(formulario, usuarioRepositorio.findByUsername(USUARIO_PRUEBA).orElseThrow().getId());
    }
}
