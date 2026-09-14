package ni.gestionvacaciones.solicitud;

import ni.gestionvacaciones.PruebaConPostgres;
import ni.gestionvacaciones.empleado.Empleado;
import ni.gestionvacaciones.empleado.EmpleadoRepositorio;
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

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El registro de solicitudes y el ajuste de saldo, de punta a punta por HTTP. */
class ControladorSolicitudesWebTest extends PruebaConPostgres {

    private static final String USUARIO_PRUEBA = "prueba-solicitudes";
    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Managua"));

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private ServicioUsuario servicioUsuario;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private ServicioEmpleados servicioEmpleados;

    @Autowired
    private EmpleadoRepositorio empleadoRepositorio;

    @Autowired
    private ServicioSolicitudes servicioSolicitudes;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        if (!usuarioRepositorio.existsByUsername(USUARIO_PRUEBA)) {
            Usuario usuario = servicioUsuario.crear(USUARIO_PRUEBA, "frase-larga-para-solicitudes", "Usuaria de Pruebas", null);
            usuario.setDebeCambiarPassword(false);
            usuarioRepositorio.save(usuario);
        }
    }

    @Test
    @DisplayName("Formulario → resumen (sin guardar) → guardar → la ficha muestra el descuento")
    void flujoCompleto() throws Exception {
        Empleado funcionario = crearFuncionario("Web Flujo Completo", 10);

        String formulario = html(get("/solicitudes/nueva").param("funcionario", funcionario.getId().toString()));
        assertThat(formulario).contains("Registrar solicitud", "Web Flujo Completo", "Revisar antes de guardar");

        String resumen = html(post("/solicitudes/confirmar").with(csrf())
                .params(vacaciones(funcionario, HOY, HOY.plusDays(4), "5")));
        assertThat(resumen).contains("Revisá antes de guardar", "Web Flujo Completo", "Vacaciones",
                "5 días", "10 días", "5 días corridos");
        assertThat(saldo(funcionario)).as("confirmar no guarda nada").isEqualTo(4800);

        String destino = mvc.perform(post("/solicitudes").with(sesion()).with(csrf())
                        .params(vacaciones(funcionario, HOY, HOY.plusDays(4), "5")))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(destino).isEqualTo("/empleados/" + funcionario.getId());
        assertThat(saldo(funcionario)).isEqualTo(2400);

        String ficha = html(get(destino));
        assertThat(ficha).contains("Registrada", "Descuento por solicitud", "Anular", "Usado en");
    }

    @Test
    @DisplayName("Saldo insuficiente: el mensaje con nombres y la casilla de autorización; sin marcarla no guarda")
    void saldoInsuficienteEnPantalla() throws Exception {
        Empleado funcionario = crearFuncionario("Web Saldo Corto", 1);

        String resumen = html(post("/solicitudes/confirmar").with(csrf())
                .params(vacaciones(funcionario, HOY, HOY.plusDays(2), "3")));
        assertThat(resumen).contains(
                "Usuaria, Web Saldo Corto solo tiene 1 día de saldo y estás registrando 3 días.",
                "Autorizo registrar esta solicitud aunque el saldo quede en negativo.");

        String reintento = html(post("/solicitudes").with(csrf())
                .params(vacaciones(funcionario, HOY, HOY.plusDays(2), "3")));
        assertThat(reintento).contains("Autorizo registrar");
        assertThat(saldo(funcionario)).isEqualTo(480);
    }

    @Test
    @DisplayName("Cita médica: la nota de no escribir datos de salud aparece en el formulario y en el resumen")
    void notaDeCitaMedica() throws Exception {
        Empleado funcionario = crearFuncionario("Web Cita Médica", 5);

        assertThat(html(get("/solicitudes/nueva"))).contains("no escribas diagnósticos");

        String resumen = html(post("/solicitudes/confirmar").with(csrf())
                .param("empleadoId", funcionario.getId().toString())
                .param("tipo", "CITA_MEDICA")
                .param("fechaInicio", HOY.toString())
                .param("fechaFin", HOY.toString())
                .param("dias", "0").param("horasSueltas", "0")
                .param("horas", "2").param("minutos", "30")
                .param("motivo", "Cita en el hospital"));
        assertThat(resumen).contains("2 horas y 30 minutos", "no se escribe ningún dato de salud");
    }

    @Test
    @DisplayName("Un número con decimales muestra un error en español, sin palabras técnicas")
    void decimalesEnEspanol() throws Exception {
        Empleado funcionario = crearFuncionario("Web Decimales", 5);

        String respuesta = html(post("/solicitudes/confirmar").with(csrf())
                .params(vacaciones(funcionario, HOY, HOY, "1.5")));
        assertThat(respuesta).contains("Escribí un número entero, sin decimales ni letras.");
        assertThat(respuesta).doesNotContain("Failed to convert", "NumberFormatException");
    }

    @Test
    @DisplayName("Anular desde la pantalla: pide motivo, y después la ficha muestra la anulación")
    void anularDesdeLaPantalla() throws Exception {
        Empleado funcionario = crearFuncionario("Web Para Anular", 6);
        Usuario usuario = usuarioRepositorio.findByUsername(USUARIO_PRUEBA).orElseThrow();
        FormularioSolicitud datos = new FormularioSolicitud();
        datos.setEmpleadoId(funcionario.getId());
        datos.setTipo(TipoSolicitud.VACACIONES);
        datos.setFechaInicio(HOY);
        datos.setFechaFin(HOY.plusDays(1));
        datos.setDias(2);
        Long solicitudId = servicioSolicitudes.registrar(datos, usuario, null).solicitud().getId();

        assertThat(html(get("/solicitudes/" + solicitudId + "/anular")))
                .contains("¿Anular la solicitud de vacaciones de Web Para Anular?", "4 días", "6 días");

        assertThat(html(post("/solicitudes/" + solicitudId + "/anular").with(csrf()).param("motivo", "")))
                .contains("Escribí el motivo de la anulación.");
        assertThat(saldo(funcionario)).isEqualTo(1920);

        mvc.perform(post("/solicitudes/" + solicitudId + "/anular").with(sesion()).with(csrf())
                        .param("motivo", "Se canceló el viaje"))
                .andExpect(status().is3xxRedirection());
        assertThat(saldo(funcionario)).isEqualTo(2880);

        assertThat(html(get("/empleados/" + funcionario.getId())))
                .contains("Anulada", "Se canceló el viaje", "Devolución por anulación");
    }

    @Test
    @DisplayName("Ajustar el saldo desde la pantalla deja el movimiento en el historial")
    void ajusteDesdeLaPantalla() throws Exception {
        Empleado funcionario = crearFuncionario("Web Ajuste", 3);

        assertThat(html(get("/empleados/" + funcionario.getId() + "/ajuste")))
                .contains("Ajustar el saldo de Web Ajuste", "Sumar tiempo", "Restar tiempo");

        mvc.perform(post("/empleados/" + funcionario.getId() + "/ajuste").with(sesion()).with(csrf())
                        .param("operacion", "SUMAR").param("dias", "1").param("horas", "0").param("minutos", "0")
                        .param("motivo", "Día compensatorio"))
                .andExpect(status().is3xxRedirection());

        assertThat(saldo(funcionario)).isEqualTo(1920);
        assertThat(html(get("/empleados/" + funcionario.getId()))).contains("Ajuste manual", "Día compensatorio");
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
        Long usuarioId = usuarioRepositorio.findByUsername(USUARIO_PRUEBA).orElseThrow().getId();
        return servicioEmpleados.crear(formulario, usuarioId);
    }

    private int saldo(Empleado funcionario) {
        return empleadoRepositorio.findById(funcionario.getId()).orElseThrow().getSaldoVacacionesMinutos();
    }

    private static org.springframework.util.MultiValueMap<String, String> vacaciones(
            Empleado funcionario, LocalDate inicio, LocalDate fin, String dias) {
        org.springframework.util.LinkedMultiValueMap<String, String> campos = new org.springframework.util.LinkedMultiValueMap<>();
        campos.add("empleadoId", funcionario.getId().toString());
        campos.add("tipo", "VACACIONES");
        campos.add("fechaInicio", inicio.toString());
        campos.add("fechaFin", fin.toString());
        campos.add("dias", dias);
        campos.add("horasSueltas", "0");
        campos.add("horas", "0");
        campos.add("minutos", "0");
        return campos;
    }
}
