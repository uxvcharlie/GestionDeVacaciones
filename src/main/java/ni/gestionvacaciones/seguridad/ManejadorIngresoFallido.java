package ni.gestionvacaciones.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Qué hacer cuando un ingreso falla: contar el intento y volver a la pantalla
 * de ingreso con el mensaje que corresponde.
 *
 * <p>El mensaje por contraseña equivocada es siempre el mismo, exista o no el
 * usuario, para no confirmarle a nadie qué nombres de usuario existen.</p>
 */
@Component
public class ManejadorIngresoFallido implements AuthenticationFailureHandler {

    private final ServicioIntentosIngreso intentos;

    public ManejadorIngresoFallido(ServicioIntentosIngreso intentos) {
        this.intentos = intentos;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest peticion, HttpServletResponse respuesta,
                                        AuthenticationException error) throws IOException {
        ServicioIntentosIngreso.Resultado resultado =
                intentos.registrarFallo(peticion.getParameter("usuario"), peticion.getRemoteAddr());

        String destino = switch (resultado) {
            case BLOQUEADO -> "/ingresar?bloqueado";
            case DESACTIVADO -> "/ingresar?desactivado";
            case CREDENCIALES_INCORRECTAS -> "/ingresar?error";
        };
        respuesta.sendRedirect(peticion.getContextPath() + destino);
    }
}
