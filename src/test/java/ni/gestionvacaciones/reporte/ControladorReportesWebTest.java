package ni.gestionvacaciones.reporte;

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
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pantalla de reportes y descargas, por HTTP. */
class ControladorReportesWebTest extends PruebaConPostgres {

    private static final String USUARIO_PRUEBA = "prueba-reportes";

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
            Usuario usuario = servicioUsuario.crear(USUARIO_PRUEBA, "frase-larga-de-reportes", "Reportes de Pruebas", null);
            usuario.setDebeCambiarPassword(false);
            usuarioRepositorio.save(usuario);
        }
    }

    @Test
    @DisplayName("La pantalla de reportes ofrece el mes actual y los dos formatos")
    void pantalla() throws Exception {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Managua"));
        String html = mvc.perform(get("/reportes").with(sesion()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Solicitudes de un mes", "Descargar Excel", "Descargar CSV",
                "value=\"" + hoy.getYear() + "\" selected");
    }

    @Test
    @DisplayName("Descargar el mes en CSV y en Excel devuelve archivos adjuntos con nombre")
    void descargasDelMes() throws Exception {
        mvc.perform(get("/reportes/mes").param("anio", "2026").param("mes", "9").param("formato", "csv").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("solicitudes-2026-09.csv")));

        byte[] excel = mvc.perform(get("/reportes/mes").param("anio", "2026").param("mes", "9").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, EscritorExcel.TIPO_CONTENIDO))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(excel).startsWith(new byte[]{'P', 'K'});
    }

    @Test
    @DisplayName("Descargar el reporte de un funcionario desde su ficha")
    void descargaDeFuncionario() throws Exception {
        FormularioEmpleado formulario = new FormularioEmpleado();
        formulario.setNombreCompleto("Web Reporte Funcionario");
        formulario.setSaldoDias(3);
        formulario.setSaldoHoras(0);
        Empleado funcionario = servicioEmpleados.crear(formulario,
                usuarioRepositorio.findByUsername(USUARIO_PRUEBA).orElseThrow().getId());

        assertThat(mvc.perform(get("/empleados/" + funcionario.getId()).with(sesion()))
                .andReturn().getResponse().getContentAsString())
                .contains("Descargar reporte", "formato=excel", "formato=csv");

        mvc.perform(get("/reportes/funcionario/" + funcionario.getId()).param("formato", "csv").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("funcionario-web-reporte-funcionario-")));
    }

    @Test
    @DisplayName("Un formato o un mes inválido da 400, y sin sesión no se descarga nada")
    void invalidos() throws Exception {
        mvc.perform(get("/reportes/mes").param("anio", "2026").param("mes", "13").with(sesion()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/reportes/mes").param("anio", "2026").param("mes", "9").param("formato", "pdf").with(sesion()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/reportes/mes").param("anio", "2026").param("mes", "9"))
                .andExpect(status().is3xxRedirection());
    }

    private RequestPostProcessor sesion() {
        return user(USUARIO_PRUEBA).roles("ADMIN");
    }
}
