package ni.gestionvacaciones.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** La base dormida muestra la pantalla amable, sin base de datos ni Spring completo. */
class ManejadorBaseDeDatosNoDisponibleTest {

    @Controller
    static class ControladorQueFalla {

        @GetMapping("/consulta")
        String consulta() {
            throw new CannotCreateTransactionException("Neon no respondió");
        }

        @PostMapping("/guardado")
        String guardado() {
            throw new DataAccessResourceFailureException("Neon no respondió");
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControladorQueFalla())
            .setControllerAdvice(new ManejadorBaseDeDatosNoDisponible())
            .build();

    @Test
    @DisplayName("En una consulta: 503, pantalla amable y reintento automático")
    void consulta() throws Exception {
        mvc.perform(get("/consulta"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error-base-de-datos"))
                .andExpect(model().attribute("reintentoAutomatico", true));
    }

    @Test
    @DisplayName("Al guardar: 503, pantalla amable y SIN reintento automático")
    void guardado() throws Exception {
        mvc.perform(post("/guardado"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error-base-de-datos"))
                .andExpect(model().attribute("reintentoAutomatico", false));
    }
}
