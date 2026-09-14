package ni.gestionvacaciones.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Renueva la sesión cuando Brenda toca "Seguir trabajando" en el aviso de
 * inactividad.
 *
 * <p>No hace falta hacer nada: cualquier petición con la sesión abierta
 * reinicia su cuenta de 30 minutos. Devuelve 204 (sin contenido) para que el
 * JavaScript sepa que la sesión seguía viva. Si ya había vencido, Spring
 * Security responde con una redirección al ingreso, y el aviso lo entiende.</p>
 */
@RestController
public class ControladorSesion {

    @GetMapping("/sesion/mantener")
    public ResponseEntity<Void> mantener() {
        return ResponseEntity.noContent().build();
    }
}
