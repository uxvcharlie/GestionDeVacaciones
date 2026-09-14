package ni.gestionvacaciones.web;

import ni.gestionvacaciones.seguridad.ServicioUsuario;
import ni.gestionvacaciones.seguridad.Usuario;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/**
 * Datos que toda pantalla necesita, puestos en un solo lugar.
 *
 * <p>{@code @ControllerAdvice} hace que este método se ejecute antes de mostrar
 * cualquier plantilla, así que en todas ellas podemos usar
 * {@code ${usuarioActual}} sin que cada controlador tenga que agregarlo.</p>
 */
@ControllerAdvice
public class ModeloGlobal {

    private final ServicioUsuario servicioUsuario;

    public ModeloGlobal(ServicioUsuario servicioUsuario) {
        this.servicioUsuario = servicioUsuario;
    }

    @ModelAttribute("usuarioActual")
    public Usuario usuarioActual(Principal principal) {
        if (principal == null) {
            return null; // pantalla de ingreso: todavía no hay nadie
        }
        return servicioUsuario.buscarPorUsername(principal.getName()).orElse(null);
    }
}
