package ni.gestionvacaciones.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ni.gestionvacaciones.comun.ReglaDeNegocioException;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import ni.gestionvacaciones.seguridad.Usuario;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Pantallas del panel de usuarios.
 *
 * <p>La contraseña temporal se pasa a la pantalla de entrega como "atributo
 * flash": vive en la sesión solo hasta la siguiente petición. Por eso se ve
 * una única vez, y recargar la página no la vuelve a mostrar.</p>
 */
@Controller
@RequestMapping("/admin/usuarios")
public class ControladorUsuarios {

    private static final String VISTA_NUEVO = "admin/usuario-nuevo";
    private static final String VOLVER_A_LA_LISTA = "redirect:/admin/usuarios";

    private final ServicioAdministracionUsuarios servicio;
    private final ServicioUsuario servicioUsuario;
    private final ServicioParametros parametros;

    public ControladorUsuarios(ServicioAdministracionUsuarios servicio,
                               ServicioUsuario servicioUsuario,
                               ServicioParametros parametros) {
        this.servicio = servicio;
        this.servicioUsuario = servicioUsuario;
        this.parametros = parametros;
    }

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("usuarios", servicio.listar());
        modelo.addAttribute("zona", parametros.zonaHoraria());
        return "admin/usuarios";
    }

    @GetMapping("/nuevo")
    public String mostrarAlta(Model modelo) {
        modelo.addAttribute("formulario", new FormularioUsuario());
        return VISTA_NUEVO;
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("formulario") FormularioUsuario formulario,
                        BindingResult errores,
                        Principal principal,
                        HttpServletRequest peticion,
                        Model modelo,
                        RedirectAttributes redireccion) {
        if (errores.hasErrors()) {
            return VISTA_NUEVO;
        }
        try {
            ServicioAdministracionUsuarios.UsuarioConPassword creado =
                    servicio.crear(formulario, usuarioActual(principal), peticion.getRemoteAddr());
            redireccion.addFlashAttribute("passwordTemporal", creado.passwordTemporal());
            redireccion.addFlashAttribute("motivo", "creado");
            return "redirect:/admin/usuarios/" + creado.usuario().getId() + "/entregar";
        } catch (ReglaDeNegocioException e) {
            modelo.addAttribute("mensajeError", e.getMessage());
            return VISTA_NUEVO;
        }
    }

    @GetMapping("/{id}/entregar")
    public String entregarPassword(@PathVariable Long id, Model modelo, RedirectAttributes redireccion) {
        if (!modelo.containsAttribute("passwordTemporal")) {
            redireccion.addFlashAttribute("mensajeAviso",
                    "La contraseña temporal se muestra una sola vez. Si se perdió, restablecela y se genera otra.");
            return VOLVER_A_LA_LISTA;
        }
        modelo.addAttribute("usuario", servicio.obtener(id));
        return "admin/entregar-password";
    }

    @GetMapping("/{id}/bloquear")
    public String confirmarBloqueo(@PathVariable Long id, Model modelo) {
        modelo.addAttribute("usuario", servicio.obtener(id));
        modelo.addAttribute("accion", "bloquear");
        return "admin/confirmar-usuario";
    }

    @PostMapping("/{id}/bloquear")
    public String bloquear(@PathVariable Long id, Principal principal, HttpServletRequest peticion,
                           RedirectAttributes redireccion) {
        try {
            Usuario usuario = servicio.bloquear(id, usuarioActual(principal), peticion.getRemoteAddr());
            redireccion.addFlashAttribute("mensajeExito", "Se bloqueó el usuario de " + usuario.getNombreCompleto()
                    + ". Ya no puede entrar, y si tenía la sesión abierta se le cierra en su próximo clic.");
        } catch (ReglaDeNegocioException e) {
            redireccion.addFlashAttribute("mensajeError", e.getMessage());
        }
        return VOLVER_A_LA_LISTA;
    }

    @PostMapping("/{id}/desbloquear")
    public String desbloquear(@PathVariable Long id, Principal principal, HttpServletRequest peticion,
                              RedirectAttributes redireccion) {
        try {
            Usuario usuario = servicio.desbloquear(id, usuarioActual(principal), peticion.getRemoteAddr());
            redireccion.addFlashAttribute("mensajeExito", "Se desbloqueó el usuario de " + usuario.getNombreCompleto()
                    + ". Ya puede volver a entrar.");
        } catch (ReglaDeNegocioException e) {
            redireccion.addFlashAttribute("mensajeAviso", e.getMessage());
        }
        return VOLVER_A_LA_LISTA;
    }

    @GetMapping("/{id}/desbloquear")
    public String desbloquearDesdeDireccion() {
        return VOLVER_A_LA_LISTA;
    }

    @GetMapping("/{id}/restablecer")
    public String confirmarRestablecer(@PathVariable Long id, Model modelo) {
        modelo.addAttribute("usuario", servicio.obtener(id));
        modelo.addAttribute("accion", "restablecer");
        return "admin/confirmar-usuario";
    }

    @PostMapping("/{id}/restablecer")
    public String restablecer(@PathVariable Long id, Principal principal, HttpServletRequest peticion,
                              RedirectAttributes redireccion) {
        try {
            ServicioAdministracionUsuarios.UsuarioConPassword restablecido =
                    servicio.restablecerPassword(id, usuarioActual(principal), peticion.getRemoteAddr());
            redireccion.addFlashAttribute("passwordTemporal", restablecido.passwordTemporal());
            redireccion.addFlashAttribute("motivo", "restablecido");
            return "redirect:/admin/usuarios/" + id + "/entregar";
        } catch (ReglaDeNegocioException e) {
            redireccion.addFlashAttribute("mensajeError", e.getMessage());
            return VOLVER_A_LA_LISTA;
        }
    }

    private Usuario usuarioActual(Principal principal) {
        return servicioUsuario.buscarPorUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario de la sesión."));
    }
}
