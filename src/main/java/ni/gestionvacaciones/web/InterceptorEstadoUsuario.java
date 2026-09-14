package ni.gestionvacaciones.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import ni.gestionvacaciones.seguridad.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;
import java.util.Set;

/**
 * Revisa, en cada pantalla, el estado ACTUAL del usuario en la base de datos.
 *
 * <ol>
 *   <li><b>Si fue bloqueado</b> por un administrador, se le cierra la sesión en
 *       ese mismo momento. Sin esto, alguien bloqueado seguiría trabajando
 *       hasta que se le venciera la sesión.</li>
 *   <li><b>Si debe cambiar la contraseña</b>, cualquier dirección lo devuelve a
 *       /cambiar-password. No puede saltárselo escribiendo una dirección.</li>
 * </ol>
 *
 * <p>Consultamos la base en cada petición en lugar de confiar en lo que quedó
 * guardado en la sesión al ingresar. Es una consulta mínima y garantiza que un
 * cambio hecho desde el panel de usuarios tenga efecto de inmediato.</p>
 */
@Component
public class InterceptorEstadoUsuario implements HandlerInterceptor {

    /** Direcciones permitidas mientras se debe cambiar la contraseña, o la persona quedaría encerrada. */
    private static final Set<String> RUTAS_PERMITIDAS = Set.of("/cambiar-password", "/salir", "/ingresar");

    private final ServicioUsuario servicioUsuario;

    /** Cierra la sesión y borra la cookie, igual que el botón "Cerrar sesión". */
    private final LogoutHandler cierreDeSesion = new CompositeLogoutHandler(
            new CookieClearingLogoutHandler("SESION"),
            new SecurityContextLogoutHandler());

    public InterceptorEstadoUsuario(ServicioUsuario servicioUsuario) {
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

        Optional<Usuario> encontrado = servicioUsuario.buscarPorUsername(autenticacion.getName());
        if (encontrado.isEmpty()) {
            return true;
        }
        Usuario usuario = encontrado.get();

        if (!usuario.isActivo()) {
            cierreDeSesion.logout(peticion, respuesta, autenticacion);
            respuesta.sendRedirect(peticion.getContextPath() + "/ingresar?desactivado");
            return false;
        }

        String ruta = peticion.getRequestURI().substring(peticion.getContextPath().length());
        if (usuario.isDebeCambiarPassword() && !RUTAS_PERMITIDAS.contains(ruta)) {
            respuesta.sendRedirect(peticion.getContextPath() + "/cambiar-password");
            return false;
        }
        return true;
    }
}
