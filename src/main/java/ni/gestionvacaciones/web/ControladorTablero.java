package ni.gestionvacaciones.web;

import ni.gestionvacaciones.empleado.Empleado;
import ni.gestionvacaciones.empleado.ServicioEmpleados;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.solicitud.TipoSolicitud;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

/** El tablero de inicio y su buscador. */
@Controller
public class ControladorTablero {

    private static final int RESULTADOS_EN_TABLERO = 8;

    private final ServicioTablero servicioTablero;
    private final ServicioEmpleados servicioEmpleados;
    private final ServicioParametros parametros;
    private final ServicioUsuario servicioUsuario;

    public ControladorTablero(ServicioTablero servicioTablero,
                              ServicioEmpleados servicioEmpleados,
                              ServicioParametros parametros,
                              ServicioUsuario servicioUsuario) {
        this.servicioTablero = servicioTablero;
        this.servicioEmpleados = servicioEmpleados;
        this.parametros = parametros;
        this.servicioUsuario = servicioUsuario;
    }

    /**
     * @param vencido presente cuando Spring Security rechazó un formulario con
     *                un token de seguridad que ya no era válido (ver
     *                ConfiguracionSeguridad). Se avisa en lugar de mostrar un error.
     */
    @GetMapping("/")
    public String mostrarTablero(@RequestParam(required = false) String vencido, Principal principal, Model modelo) {
        Usuario usuario = servicioUsuario.buscarPorUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario de la sesión."));

        modelo.addAttribute("resumen", servicioTablero.resumen(usuario));
        modelo.addAttribute("tipos", TipoSolicitud.values());
        modelo.addAttribute("horasPorJornada", parametros.horasPorJornada());
        if (vencido != null) {
            modelo.addAttribute("mensajeAviso",
                    "Por seguridad, ese formulario ya no era válido y no se guardó nada. Volvé a intentarlo.");
        }
        return "tablero";
    }

    /**
     * Resultados del buscador del tablero. Lo pide HTMX mientras se escribe y
     * devuelve solo el pedazo de HTML de los resultados, no la página entera.
     */
    @GetMapping("/tablero/buscar")
    public String buscar(@RequestParam(name = "q", defaultValue = "") String busqueda, Model modelo) {
        List<Empleado> encontrados = busqueda.isBlank() ? List.of() : servicioEmpleados.buscar(busqueda, false);
        modelo.addAttribute("q", busqueda);
        modelo.addAttribute("totalEncontrados", encontrados.size());
        modelo.addAttribute("encontrados", encontrados.stream().limit(RESULTADOS_EN_TABLERO).toList());
        modelo.addAttribute("hayMas", encontrados.size() > RESULTADOS_EN_TABLERO);
        modelo.addAttribute("horasPorJornada", parametros.horasPorJornada());
        return "fragmentos/busqueda-tablero :: resultados";
    }
}
