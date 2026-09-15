package ni.gestionvacaciones.web;

import ni.gestionvacaciones.PruebaConPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Con HTTPS exigido (como en producción): redirige todo menos el chequeo de salud. */
@TestPropertySource(properties = "app.seguridad.exigir-https=true")
class ProduccionHttpsTest extends PruebaConPostgres {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Una petición por HTTP se redirige a HTTPS")
    void redirigeAHttps() throws Exception {
        mvc.perform(get("http://gestion.ejemplo.com/ingresar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://gestion.ejemplo.com/ingresar"));
    }

    @Test
    @DisplayName("Por HTTPS no se redirige")
    void httpsNoRedirige() throws Exception {
        mvc.perform(get("https://gestion.ejemplo.com/ingresar").secure(true))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("El chequeo de salud responde por HTTP, sin redirigir, y sin tocar la base")
    void saludSinRedireccion() throws Exception {
        mvc.perform(get("http://gestion.ejemplo.com/actuator/health/liveness"))
                .andExpect(status().isOk());
    }
}
