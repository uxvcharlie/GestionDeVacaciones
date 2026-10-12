package ni.gestionvacaciones.web;

import ni.gestionvacaciones.PruebaConPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La aplicación instalable en el celular: los archivos que el teléfono pide
 * antes de ingresar tienen que ser públicos, y la pantalla de ingreso tiene que
 * anunciarlos. Si alguno pidiera sesión, el celular no podría instalarla.
 */
class AplicacionInstalableWebTest extends PruebaConPostgres {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Manifiesto, service worker, pantalla sin conexión e íconos se sirven sin sesión")
    void archivosPublicos() throws Exception {
        String manifiesto = mvc.perform(get("/manifest.json"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(manifiesto).contains("\"display\": \"standalone\"", "icono-maskable-512.png");

        mvc.perform(get("/sw.js")).andExpect(status().isOk());
        mvc.perform(get("/sin-conexion.html")).andExpect(status().isOk());
        mvc.perform(get("/iconos/icono-512.png")).andExpect(status().isOk());
        mvc.perform(get("/iconos/apple-touch-icon.png")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("La pantalla de ingreso anuncia el manifiesto y el ícono del iPhone")
    void ingresoAnunciaLaAplicacion() throws Exception {
        String pantalla = mvc.perform(get("/ingresar"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(pantalla).contains("rel=\"manifest\"", "apple-touch-icon", "theme-color");
    }

    @Test
    @DisplayName("La política de seguridad permite el manifiesto y el service worker propios")
    void politicaDeSeguridad() throws Exception {
        mvc.perform(get("/ingresar"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("manifest-src 'self'"),
                                org.hamcrest.Matchers.containsString("worker-src 'self'"))));
    }

    @Test
    @DisplayName("Las pantallas con datos siguen exigiendo haber ingresado")
    void datosSiguenProtegidos() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection());
    }
}
