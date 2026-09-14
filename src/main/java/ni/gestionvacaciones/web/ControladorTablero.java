package ni.gestionvacaciones.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * La pantalla principal.
 *
 * <p>En esta fase es apenas una pantalla de bienvenida que demuestra que el
 * ingreso funciona. El tablero de verdad —saludo según la hora, buscador,
 * tarjetas de resumen y últimas solicitudes— se construye en la Fase 4, cuando
 * ya existan empleados (Fase 2) y solicitudes (Fase 3) que mostrar.</p>
 */
@Controller
public class ControladorTablero {

    @GetMapping("/")
    public String mostrarTablero() {
        return "tablero";
    }
}
