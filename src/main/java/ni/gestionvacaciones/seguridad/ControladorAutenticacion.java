package ni.gestionvacaciones.seguridad;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

/**
 * Las dos pantallas relacionadas con el acceso: ingresar y cambiar contraseña.
 *
 * <p>Ojo: el formulario de ingreso NO se procesa acá. Spring Security
 * intercepta el POST a /ingresar antes de que llegue a ningún controlador.
 * Nosotros solo mostramos la pantalla.</p>
 */
@Controller
public class ControladorAutenticacion {

    private final ServicioUsuario servicioUsuario;
    private final PoliticaPassword politica;

    public ControladorAutenticacion(ServicioUsuario servicioUsuario, PoliticaPassword politica) {
        this.servicioUsuario = servicioUsuario;
        this.politica = politica;
    }

    @GetMapping("/ingresar")
    public String mostrarIngreso(@RequestParam(required = false) String error,
                                 @RequestParam(required = false) String salida,
                                 @RequestParam(required = false) String expirada,
                                 Principal principal,
                                 Model modelo) {
        // Si ya está adentro, no tiene sentido mostrarle el login otra vez.
        if (principal != null) {
            return "redirect:/";
        }
        if (error != null) {
            // Mensaje genérico a propósito: no revelamos si el usuario existe.
            modelo.addAttribute("mensajeError", "Usuario o contraseña incorrectos.");
        }
        if (salida != null) {
            modelo.addAttribute("mensajeAviso", "Cerraste tu sesión. Hasta luego.");
        }
        if (expirada != null) {
            modelo.addAttribute("mensajeAviso",
                    "Tu sesión se cerró por inactividad. Volvé a ingresar.");
        }
        return "login";
    }

    @GetMapping("/cambiar-password")
    public String mostrarCambioPassword(Model modelo) {
        if (!modelo.containsAttribute("formulario")) {
            modelo.addAttribute("formulario", new FormularioCambioPassword());
        }
        modelo.addAttribute("explicacionPassword", politica.explicacion());
        return "cambiar-password";
    }

    @PostMapping("/cambiar-password")
    public String procesarCambioPassword(@Valid @ModelAttribute("formulario") FormularioCambioPassword formulario,
                                         BindingResult errores,
                                         Principal principal,
                                         Model modelo,
                                         RedirectAttributes redireccion) {
        modelo.addAttribute("explicacionPassword", politica.explicacion());

        if (errores.hasErrors()) {
            return "cambiar-password";
        }

        Optional<String> problema = servicioUsuario.cambiarPassword(
                principal.getName(),
                formulario.getActual(),
                formulario.getNueva(),
                formulario.getConfirmacion());

        if (problema.isPresent()) {
            modelo.addAttribute("mensajeError", problema.get());
            return "cambiar-password";
        }

        redireccion.addFlashAttribute("mensajeExito", "Listo, tu contraseña quedó cambiada.");
        return "redirect:/";
    }
}
