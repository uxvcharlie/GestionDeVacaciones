package ni.gestionvacaciones.solicitud;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Se pidió una solicitud que no existe. Se muestra la página 404 propia. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class SolicitudNoEncontradaException extends RuntimeException {

    public SolicitudNoEncontradaException(Long id) {
        super("No existe la solicitud con id " + id);
    }
}
