package ni.gestionvacaciones.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Qué hacer cuando alguien ingresa bien: reiniciar sus intentos fallidos,
 * anotar el ingreso en la bitácora y llevarlo siempre al tablero.
 *
 * <p>La protección contra la fijación de sesión ya actuó antes de llegar
 * acá: la sesión con la que sigue es nueva.</p>
 */
@Component
public class ManejadorIngresoExitoso implements AuthenticationSuccessHandler {

    private final ServicioIntentosIngreso intentos;

    public ManejadorIngresoExitoso(ServicioIntentosIngreso intentos) {
        this.intentos = intentos;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest peticion, HttpServletResponse respuesta,
                                        Authentication autenticacion) throws IOException {
        intentos.registrarExito(autenticacion.getName(), peticion.getRemoteAddr());
        respuesta.sendRedirect(peticion.getContextPath() + "/");
    }
}
