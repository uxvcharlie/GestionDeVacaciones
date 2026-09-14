package ni.gestionvacaciones.web;

import jakarta.servlet.http.HttpServletRequest;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import ni.gestionvacaciones.seguridad.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.time.Duration;

/**
 * Datos que toda pantalla necesita, puestos en un solo lugar.
 *
 * <p>{@code @ControllerAdvice} hace que estos métodos se ejecuten antes de
 * mostrar cualquier plantilla, así que en todas ellas se pueden usar sin que
 * cada controlador tenga que agregarlos.</p>
 */
@ControllerAdvice
public class ModeloGlobal {

    private final ServicioUsuario servicioUsuario;
    private final long minutosSesion;

    public ModeloGlobal(ServicioUsuario servicioUsuario,
                        @Value("${server.servlet.session.timeout:30m}") Duration duracionSesion) {
        this.servicioUsuario = servicioUsuario;
        this.minutosSesion = duracionSesion.toMinutes();
    }

    @ModelAttribute("usuarioActual")
    public Usuario usuarioActual(Principal principal) {
        if (principal == null) {
            return null; // pantalla de ingreso: todavía no hay nadie
        }
        return servicioUsuario.buscarPorUsername(principal.getName()).orElse(null);
    }

    /**
     * Qué sección del menú está activa, para marcarla con aria-current="page".
     * Así un lector de pantalla anuncia "Funcionarios, página actual".
     */
    @ModelAttribute("seccion")
    public String seccion(HttpServletRequest peticion) {
        String ruta = peticion.getRequestURI().substring(peticion.getContextPath().length());
        if (ruta.startsWith("/admin")) {
            return "admin";
        }
        return ruta.startsWith("/empleados") || ruta.startsWith("/solicitudes") ? "empleados" : "inicio";
    }

    /**
     * Cuántos minutos dura la sesión sin actividad. Lo usa el aviso que aparece
     * un minuto antes de que se cierre. Sale de la misma configuración que usa
     * el servidor, así los dos relojes nunca dicen cosas distintas.
     */
    @ModelAttribute("minutosSesion")
    public long minutosSesion() {
        return minutosSesion;
    }
}
