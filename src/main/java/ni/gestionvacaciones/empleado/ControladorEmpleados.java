package ni.gestionvacaciones.empleado;

import jakarta.validation.Valid;
import ni.gestionvacaciones.comun.ConversorTiempo;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.saldo.MovimientoSaldo;
import ni.gestionvacaciones.saldo.ServicioSaldo;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import ni.gestionvacaciones.solicitud.ServicioSolicitudes;
import ni.gestionvacaciones.solicitud.Solicitud;
import ni.gestionvacaciones.solicitud.TipoSolicitud;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pantallas de funcionarios: listado con buscador, alta, ficha, edición y baja.
 *
 * <p>Toda acción que cambia datos es un POST con token CSRF. Después de
 * guardar se redirige (patrón POST → redirección → GET), así recargar la
 * página nunca vuelve a enviar el formulario.</p>
 */
@Controller
@RequestMapping("/empleados")
public class ControladorEmpleados {

    private static final String VISTA_FORMULARIO = "empleados/formulario";

    private final ServicioEmpleados servicioEmpleados;
    private final ServicioSaldo servicioSaldo;
    private final ServicioSolicitudes servicioSolicitudes;
    private final ServicioParametros parametros;
    private final ServicioUsuario servicioUsuario;

    public ControladorEmpleados(ServicioEmpleados servicioEmpleados,
                                ServicioSaldo servicioSaldo,
                                ServicioSolicitudes servicioSolicitudes,
                                ServicioParametros parametros,
                                ServicioUsuario servicioUsuario) {
        this.servicioEmpleados = servicioEmpleados;
        this.servicioSaldo = servicioSaldo;
        this.servicioSolicitudes = servicioSolicitudes;
        this.parametros = parametros;
        this.servicioUsuario = servicioUsuario;
    }

    /**
     * Listado. Con HTMX, el buscador pide esta misma página mientras se escribe
     * y reemplaza solo la parte de resultados. Sin JavaScript, funciona igual
     * como un formulario común.
     */
    @GetMapping
    public String listar(@RequestParam(name = "q", defaultValue = "") String busqueda,
                         @RequestParam(name = "inactivos", defaultValue = "false") boolean incluirDadosDeBaja,
                         Model modelo) {
        modelo.addAttribute("empleados", servicioEmpleados.buscar(busqueda, incluirDadosDeBaja));
        modelo.addAttribute("hayEmpleados", servicioEmpleados.hayEmpleados());
        modelo.addAttribute("q", busqueda);
        modelo.addAttribute("inactivos", incluirDadosDeBaja);
        modelo.addAttribute("horasPorJornada", parametros.horasPorJornada());
        return "empleados/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarAlta(Model modelo) {
        modelo.addAttribute("formulario", new FormularioEmpleado());
        prepararFormulario(modelo, null);
        return VISTA_FORMULARIO;
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("formulario") FormularioEmpleado formulario,
                        BindingResult errores,
                        Principal principal,
                        Model modelo,
                        RedirectAttributes redireccion) {
        if (errores.hasErrors()) {
            prepararFormulario(modelo, null);
            return VISTA_FORMULARIO;
        }

        Empleado empleado = servicioEmpleados.crear(formulario, idUsuarioActual(principal));

        redireccion.addFlashAttribute("mensajeExito", "Listo, se registró a " + empleado.getNombreCompleto()
                + " con un saldo de "
                + ConversorTiempo.formatear(empleado.getSaldoVacacionesMinutos(), parametros.horasPorJornada())
                + ".");
        return "redirect:/empleados/" + empleado.getId();
    }

    /** Ficha: datos, saldo, desglose del año, solicitudes y movimientos. */
    @GetMapping("/{id}")
    public String mostrarFicha(@PathVariable Long id, Model modelo) {
        Empleado empleado = servicioEmpleados.obtener(id);
        List<MovimientoSaldo> movimientos = servicioSaldo.historial(id);
        List<Solicitud> solicitudes = servicioSolicitudes.historial(id);

        Set<Long> usuarios = new HashSet<>();
        movimientos.forEach(movimiento -> usuarios.add(movimiento.getRealizadoPor()));
        solicitudes.forEach(solicitud -> {
            usuarios.add(solicitud.getRegistradoPor());
            if (solicitud.getAnuladoPor() != null) {
                usuarios.add(solicitud.getAnuladoPor());
            }
        });

        modelo.addAttribute("empleado", empleado);
        modelo.addAttribute("movimientos", movimientos);
        modelo.addAttribute("solicitudes", solicitudes);
        modelo.addAttribute("desglose", servicioSolicitudes.desgloseDelAnio(id));
        modelo.addAttribute("tipos", TipoSolicitud.values());
        modelo.addAttribute("nombresUsuarios", servicioUsuario.nombresPorId(usuarios));
        modelo.addAttribute("horasPorJornada", parametros.horasPorJornada());
        modelo.addAttribute("zona", parametros.zonaHoraria());
        return "empleados/ficha";
    }

    @GetMapping("/{id}/editar")
    public String mostrarEdicion(@PathVariable Long id, Model modelo) {
        Empleado empleado = servicioEmpleados.obtener(id);
        modelo.addAttribute("formulario", FormularioEmpleado.desde(empleado));
        prepararFormulario(modelo, empleado);
        return VISTA_FORMULARIO;
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("formulario") FormularioEmpleado formulario,
                             BindingResult errores,
                             Model modelo,
                             RedirectAttributes redireccion) {
        if (errores.hasErrors()) {
            prepararFormulario(modelo, servicioEmpleados.obtener(id));
            return VISTA_FORMULARIO;
        }

        Empleado empleado = servicioEmpleados.actualizar(id, formulario);
        redireccion.addFlashAttribute("mensajeExito", "Se guardaron los cambios de " + empleado.getNombreCompleto() + ".");
        return "redirect:/empleados/" + id;
    }

    /** Pantalla de confirmación, con el nombre explícito, antes de dar de baja. */
    @GetMapping("/{id}/baja")
    public String confirmarBaja(@PathVariable Long id, Model modelo) {
        Empleado empleado = servicioEmpleados.obtener(id);
        if (!empleado.isActivo()) {
            return "redirect:/empleados/" + id;
        }
        modelo.addAttribute("empleado", empleado);
        return "empleados/baja";
    }

    @PostMapping("/{id}/baja")
    public String darDeBaja(@PathVariable Long id, RedirectAttributes redireccion) {
        Empleado empleado = servicioEmpleados.darDeBaja(id);
        redireccion.addFlashAttribute("mensajeExito",
                "Se dio de baja a " + empleado.getNombreCompleto() + ". Su historial se conserva.");
        return "redirect:/empleados/" + id;
    }

    /** @param empleado {@code null} al agregar; el funcionario actual al editar. */
    private void prepararFormulario(Model modelo, Empleado empleado) {
        modelo.addAttribute("empleado", empleado);
        modelo.addAttribute("horasPorJornada", parametros.horasPorJornada());
    }

    private Long idUsuarioActual(Principal principal) {
        return servicioUsuario.buscarPorUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario de la sesión."))
                .getId();
    }
}
