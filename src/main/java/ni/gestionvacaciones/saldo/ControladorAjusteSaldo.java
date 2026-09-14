package ni.gestionvacaciones.saldo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ni.gestionvacaciones.comun.ConversorTiempo;
import ni.gestionvacaciones.comun.ReglaDeNegocioException;
import ni.gestionvacaciones.empleado.Empleado;
import ni.gestionvacaciones.empleado.ServicioEmpleados;
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

/** Ajuste manual del saldo de un funcionario: sumar o restar, siempre con motivo. */
@Controller
@RequestMapping("/empleados/{id}/ajuste")
public class ControladorAjusteSaldo {

    private static final String VISTA = "empleados/ajuste";

    private final ServicioSaldo servicioSaldo;
    private final ServicioEmpleados servicioEmpleados;
    private final ServicioParametros parametros;
    private final ServicioUsuario servicioUsuario;

    public ControladorAjusteSaldo(ServicioSaldo servicioSaldo,
                                  ServicioEmpleados servicioEmpleados,
                                  ServicioParametros parametros,
                                  ServicioUsuario servicioUsuario) {
        this.servicioSaldo = servicioSaldo;
        this.servicioEmpleados = servicioEmpleados;
        this.parametros = parametros;
        this.servicioUsuario = servicioUsuario;
    }

    @GetMapping
    public String mostrar(@PathVariable Long id, Model modelo) {
        Empleado empleado = servicioEmpleados.obtener(id);
        if (!empleado.isActivo()) {
            return "redirect:/empleados/" + id;
        }
        modelo.addAttribute("formulario", new FormularioAjuste());
        preparar(modelo, empleado, false);
        return VISTA;
    }

    @PostMapping
    public String guardar(@PathVariable Long id,
                          @Valid @ModelAttribute("formulario") FormularioAjuste formulario,
                          BindingResult errores,
                          Principal principal,
                          HttpServletRequest peticion,
                          Model modelo,
                          RedirectAttributes redireccion) {
        if (errores.hasErrors()) {
            preparar(modelo, servicioEmpleados.obtener(id), false);
            return VISTA;
        }
        Usuario usuario = servicioUsuario.buscarPorUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario de la sesión."));
        try {
            CambioDeSaldo cambio = servicioSaldo.ajustarManual(id, formulario, usuario, peticion.getRemoteAddr());
            int jornada = parametros.horasPorJornada();
            redireccion.addFlashAttribute("mensajeExito", "Listo, se ajustó el saldo de "
                    + cambio.empleado().getNombreCompleto() + ": "
                    + ConversorTiempo.formatear(cambio.saldoAntes(), jornada) + " → "
                    + ConversorTiempo.formatear(cambio.saldoDespues(), jornada) + ".");
            return "redirect:/empleados/" + id;
        } catch (ReglaDeNegocioException e) {
            modelo.addAttribute("mensajeError", e.getMessage());
            preparar(modelo, servicioEmpleados.obtener(id), e.requiereAutorizacion());
            return VISTA;
        }
    }

    private void preparar(Model modelo, Empleado empleado, boolean pedirAutorizacion) {
        modelo.addAttribute("empleado", empleado);
        modelo.addAttribute("horasPorJornada", parametros.horasPorJornada());
        modelo.addAttribute("pedirAutorizacion", pedirAutorizacion);
    }
}
