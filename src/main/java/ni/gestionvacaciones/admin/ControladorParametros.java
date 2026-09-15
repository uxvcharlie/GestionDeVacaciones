package ni.gestionvacaciones.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ni.gestionvacaciones.comun.ConversorTiempo;
import ni.gestionvacaciones.comun.ReglaDeNegocioException;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Pantalla de parámetros.
 *
 * <p>Cambiar la jornada no toca ningún saldo guardado (están en minutos), pero
 * sí cambia cómo se ven todos. Por eso, antes de guardar ese cambio, se
 * muestra un ejemplo concreto y se pide confirmación.</p>
 */
@Controller
@RequestMapping("/admin/parametros")
public class ControladorParametros {

    private static final String VISTA = "admin/parametros";

    private final ServicioParametros parametros;
    private final ServicioUsuario servicioUsuario;

    public ControladorParametros(ServicioParametros parametros, ServicioUsuario servicioUsuario) {
        this.parametros = parametros;
        this.servicioUsuario = servicioUsuario;
    }

    @GetMapping
    public String mostrar(Model modelo) {
        FormularioParametros formulario = new FormularioParametros();
        formulario.setHorasPorJornada(parametros.horasPorJornada());
        formulario.setZonaHoraria(parametros.zonaHoraria().getId());
        modelo.addAttribute("formulario", formulario);
        return VISTA;
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("formulario") FormularioParametros formulario,
                          BindingResult errores,
                          Principal principal,
                          HttpServletRequest peticion,
                          Model modelo,
                          RedirectAttributes redireccion) {
        if (errores.hasErrors()) {
            return VISTA;
        }

        int horasActuales = parametros.horasPorJornada();
        if (formulario.getHorasPorJornada() != horasActuales && !formulario.isConfirmado()) {
            int ejemplo = 10 * horasActuales * ConversorTiempo.MINUTOS_POR_HORA;
            modelo.addAttribute("horasActuales", horasActuales);
            modelo.addAttribute("ejemploAntes", ConversorTiempo.formatear(ejemplo, horasActuales));
            modelo.addAttribute("ejemploDespues", ConversorTiempo.formatear(ejemplo, formulario.getHorasPorJornada()));
            return "admin/confirmar-parametros";
        }

        Long usuarioId = servicioUsuario.buscarPorUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario de la sesión."))
                .getId();
        try {
            boolean cambio = parametros.actualizar(formulario.getHorasPorJornada(), formulario.getZonaHoraria(),
                    usuarioId, peticion.getRemoteAddr());
            redireccion.addFlashAttribute("mensajeExito", cambio
                    ? "Listo, se guardaron los parámetros. Ya se están usando en todo el sistema."
                    : "No había cambios que guardar.");
            return "redirect:/admin/parametros";
        } catch (ReglaDeNegocioException e) {
            modelo.addAttribute("mensajeError", e.getMessage());
            return VISTA;
        }
    }
}
