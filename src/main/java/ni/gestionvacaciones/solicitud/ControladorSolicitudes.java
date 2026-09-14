package ni.gestionvacaciones.solicitud;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ni.gestionvacaciones.comun.ConversorTiempo;
import ni.gestionvacaciones.comun.ReglaDeNegocioException;
import ni.gestionvacaciones.empleado.Empleado;
import ni.gestionvacaciones.empleado.ServicioEmpleados;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.saldo.CambioDeSaldo;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Registrar una solicitud en dos pantallas y un clic:
 * <ol>
 *   <li>Formulario: funcionario, tipo, fechas, cantidad y motivo.</li>
 *   <li>Confirmación: el resumen y cómo queda el saldo. Nada se guarda antes.</li>
 *   <li>Guardar.</li>
 * </ol>
 * Y anular, con confirmación y motivo obligatorio.
 */
@Controller
@RequestMapping("/solicitudes")
public class ControladorSolicitudes {

    private static final String VISTA_FORMULARIO = "solicitudes/formulario";
    private static final String VISTA_CONFIRMAR = "solicitudes/confirmar";
    private static final String VISTA_ANULAR = "solicitudes/anular";
    private static final Locale ESPANOL = Locale.forLanguageTag("es-NI");

    private final ServicioSolicitudes servicioSolicitudes;
    private final ServicioEmpleados servicioEmpleados;
    private final ServicioParametros parametros;
    private final ServicioUsuario servicioUsuario;

    public ControladorSolicitudes(ServicioSolicitudes servicioSolicitudes,
                                  ServicioEmpleados servicioEmpleados,
                                  ServicioParametros parametros,
                                  ServicioUsuario servicioUsuario) {
        this.servicioSolicitudes = servicioSolicitudes;
        this.servicioEmpleados = servicioEmpleados;
        this.parametros = parametros;
        this.servicioUsuario = servicioUsuario;
    }

    /**
     * Formulario vacío. Llega con ?funcionario=ID desde la ficha, y entonces el
     * funcionario ya viene elegido. Vacaciones y la fecha de hoy vienen
     * marcadas, porque es el caso más común.
     */
    @GetMapping("/nueva")
    public String nueva(@RequestParam(name = "funcionario", required = false) Long empleadoId, Model modelo) {
        FormularioSolicitud formulario = new FormularioSolicitud();
        formulario.setEmpleadoId(empleadoId);
        formulario.setTipo(TipoSolicitud.VACACIONES);
        LocalDate hoy = LocalDate.now(parametros.zonaHoraria());
        formulario.setFechaInicio(hoy);
        formulario.setFechaFin(hoy);
        modelo.addAttribute("formulario", formulario);
        prepararFormulario(modelo);
        return VISTA_FORMULARIO;
    }

    @PostMapping("/confirmar")
    public String confirmar(@Valid @ModelAttribute("formulario") FormularioSolicitud formulario,
                            BindingResult errores,
                            Principal principal,
                            Model modelo) {
        if (errores.hasErrors()) {
            prepararFormulario(modelo);
            return VISTA_FORMULARIO;
        }
        EvaluacionSolicitud evaluacion = servicioSolicitudes.evaluar(formulario, usuarioActual(principal));
        if (evaluacion.tieneErrores()) {
            modelo.addAttribute("erroresNegocio", evaluacion.errores());
            prepararFormulario(modelo);
            return VISTA_FORMULARIO;
        }
        prepararConfirmacion(modelo, evaluacion);
        return VISTA_CONFIRMAR;
    }

    /** "Corregir datos" desde la confirmación: vuelve al formulario con todo lo escrito. */
    @PostMapping("/corregir")
    public String corregir(@ModelAttribute("formulario") FormularioSolicitud formulario, Model modelo) {
        prepararFormulario(modelo);
        return VISTA_FORMULARIO;
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("formulario") FormularioSolicitud formulario,
                          BindingResult errores,
                          Principal principal,
                          HttpServletRequest peticion,
                          Model modelo,
                          RedirectAttributes redireccion) {
        if (errores.hasErrors()) {
            prepararFormulario(modelo);
            return VISTA_FORMULARIO;
        }
        Usuario usuario = usuarioActual(principal);
        ResultadoSolicitud resultado;
        try {
            resultado = servicioSolicitudes.registrar(formulario, usuario, peticion.getRemoteAddr());
        } catch (ReglaDeNegocioException e) {
            EvaluacionSolicitud evaluacion = servicioSolicitudes.evaluar(formulario, usuario);
            if (evaluacion.tieneErrores()) {
                modelo.addAttribute("erroresNegocio", evaluacion.errores());
                prepararFormulario(modelo);
                return VISTA_FORMULARIO;
            }
            // El aviso de saldo insuficiente ya se muestra en su propia sección.
            if (!e.getMessage().equals(evaluacion.mensajeSaldoInsuficiente())) {
                modelo.addAttribute("mensajeError", e.getMessage());
            }
            prepararConfirmacion(modelo, evaluacion);
            return VISTA_CONFIRMAR;
        }

        Solicitud solicitud = resultado.solicitud();
        CambioDeSaldo cambio = resultado.cambio();
        int jornada = parametros.horasPorJornada();
        redireccion.addFlashAttribute("mensajeExito",
                "Listo, quedó registrada la solicitud de "
                        + solicitud.getTipo().getEtiqueta().toLowerCase(ESPANOL)
                        + " de " + cambio.empleado().getNombreCompleto() + ": "
                        + solicitud.getTipo().formatear(solicitud.getMinutosSolicitados(), jornada)
                        + ". Saldo: " + ConversorTiempo.formatear(cambio.saldoAntes(), jornada)
                        + " → " + ConversorTiempo.formatear(cambio.saldoDespues(), jornada) + ".");
        return "redirect:/empleados/" + solicitud.getEmpleadoId();
    }

    @GetMapping("/{id}/anular")
    public String confirmarAnulacion(@PathVariable Long id, Model modelo, RedirectAttributes redireccion) {
        Solicitud solicitud = servicioSolicitudes.obtener(id);
        if (!solicitud.isRegistrada()) {
            redireccion.addFlashAttribute("mensajeAviso", "Esa solicitud ya estaba anulada.");
            return "redirect:/empleados/" + solicitud.getEmpleadoId();
        }
        modelo.addAttribute("formularioAnulacion", new FormularioAnulacion());
        prepararAnulacion(modelo, solicitud);
        return VISTA_ANULAR;
    }

    @PostMapping("/{id}/anular")
    public String anular(@PathVariable Long id,
                         @Valid @ModelAttribute("formularioAnulacion") FormularioAnulacion formulario,
                         BindingResult errores,
                         Principal principal,
                         HttpServletRequest peticion,
                         Model modelo,
                         RedirectAttributes redireccion) {
        Solicitud solicitud = servicioSolicitudes.obtener(id);
        if (errores.hasErrors()) {
            prepararAnulacion(modelo, solicitud);
            return VISTA_ANULAR;
        }
        try {
            ResultadoSolicitud resultado = servicioSolicitudes.anular(
                    id, formulario.getMotivo(), usuarioActual(principal), peticion.getRemoteAddr());
            int jornada = parametros.horasPorJornada();
            CambioDeSaldo cambio = resultado.cambio();
            redireccion.addFlashAttribute("mensajeExito",
                    "Se anuló la solicitud y se devolvieron "
                            + solicitud.getTipo().formatear(solicitud.getMinutosSolicitados(), jornada)
                            + " al saldo de " + cambio.empleado().getNombreCompleto() + ": "
                            + ConversorTiempo.formatear(cambio.saldoAntes(), jornada)
                            + " → " + ConversorTiempo.formatear(cambio.saldoDespues(), jornada) + ".");
        } catch (ReglaDeNegocioException e) {
            redireccion.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/empleados/" + solicitud.getEmpleadoId();
    }

    // -------------------------------------------------------------------------

    private void prepararFormulario(Model modelo) {
        modelo.addAttribute("funcionarios", servicioEmpleados.buscar("", false));
        modelo.addAttribute("tipos", TipoSolicitud.values());
        modelo.addAttribute("horasPorJornada", parametros.horasPorJornada());
    }

    private void prepararConfirmacion(Model modelo, EvaluacionSolicitud evaluacion) {
        modelo.addAttribute("evaluacion", evaluacion);
        modelo.addAttribute("horasPorJornada", parametros.horasPorJornada());
    }

    private void prepararAnulacion(Model modelo, Solicitud solicitud) {
        Empleado empleado = servicioEmpleados.obtener(solicitud.getEmpleadoId());
        modelo.addAttribute("solicitud", solicitud);
        modelo.addAttribute("empleado", empleado);
        modelo.addAttribute("saldoDespues", empleado.getSaldoVacacionesMinutos() + solicitud.getMinutosSolicitados());
        modelo.addAttribute("horasPorJornada", parametros.horasPorJornada());
    }

    private Usuario usuarioActual(Principal principal) {
        return servicioUsuario.buscarPorUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario de la sesión."));
    }
}
