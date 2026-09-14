package ni.gestionvacaciones.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Obliga a cambiar la contraseña antes de usar cualquier otra pantalla.
 *
 * <p>Si el usuario tiene {@code debe_cambiar_password = true}, cualquier
 * dirección a la que vaya lo devuelve a /cambiar-password. No puede saltárselo
 * escribiendo una dirección a mano.</p>
 *
 * <p>Consultamos la base de datos en cada petición en lugar de confiar en el
 * dato guardado en la sesión. Es una consulta mínima y nos ahorra un problema
 * clásico: que la persona cambie su contraseña y el sistema siga creyendo, por
 * tener el dato viejo en memoria, que todavía debe cambiarla.</p>
 */
@Component
public class InterceptorCambioPassword implements HandlerInterceptor {

    /** Direcciones que siempre se permiten, o la persona quedaría encerrada. */
    private static final Set<String> RUTAS_PERMITIDAS = Set.of(
            "/cambiar-password", "/salir", "/ingresar");

    private final ServicioUsuario servicioUsuario;

    public InterceptorCambioPassword(ServicioUsuario servicioUsuario) {
        this.servicioUsuario = servicioUsuario;
    }

    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object handler)
            throws Exception {

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || "anonymousUser".equals(autenticacion.getPrincipal())) {
            return true; // todavía no ingresó: de esto se encarga Spring Security
        }

        String ruta = peticion.getRequestURI();
        if (RUTAS_PERMITIDAS.contains(ruta)) {
            return true;
        }

        boolean debeCambiar = servicioUsuario.buscarPorUsername(autenticacion.getName())
                .map(usuario -> usuario.isDebeCambiarPassword())
                .orElse(false);

        if (debeCambiar) {
            respuesta.sendRedirect(peticion.getContextPath() + "/cambiar-password");
            return false; // no sigue hacia el controlador
        }
        return true;
    }
}
