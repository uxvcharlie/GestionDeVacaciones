package ni.gestionvacaciones.admin;

import ni.gestionvacaciones.auditoria.AccionAuditoria;
import ni.gestionvacaciones.parametro.ServicioParametros;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/** Pantalla de bitácora: solo lectura. Nada de lo que muestra se puede editar ni borrar. */
@Controller
public class ControladorBitacora {

    private final ServicioBitacora servicioBitacora;
    private final ServicioParametros parametros;

    public ControladorBitacora(ServicioBitacora servicioBitacora, ServicioParametros parametros) {
        this.servicioBitacora = servicioBitacora;
        this.parametros = parametros;
    }

    @GetMapping("/admin/bitacora")
    public String ver(@RequestParam(required = false) String accion,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                      @RequestParam(defaultValue = "0") int pagina,
                      Model modelo) {
        String accionElegida = accion == null || accion.isBlank() ? null : accion;
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            modelo.addAttribute("mensajeError", "La fecha «hasta» no puede ser anterior a «desde». Se muestran todas las fechas.");
            desde = null;
            hasta = null;
        }
        modelo.addAttribute("pagina", servicioBitacora.buscar(accionElegida, desde, hasta, pagina));
        modelo.addAttribute("acciones", AccionAuditoria.values());
        modelo.addAttribute("accion", accionElegida);
        modelo.addAttribute("desde", desde);
        modelo.addAttribute("hasta", hasta);
        modelo.addAttribute("zona", parametros.zonaHoraria());
        return "admin/bitacora";
    }
}
