package ni.gestionvacaciones.empleado;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se pidió un empleado que no existe (por ejemplo, alguien escribió a mano
 * /empleados/9999). Spring la convierte en un 404 y se muestra la página de
 * error propia, sin detalles técnicos.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EmpleadoNoEncontradoException extends RuntimeException {

    public EmpleadoNoEncontradoException(Long id) {
        super("No existe el empleado con id " + id);
    }
}
