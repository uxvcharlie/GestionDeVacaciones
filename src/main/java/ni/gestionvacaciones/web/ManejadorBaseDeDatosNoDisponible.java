package ni.gestionvacaciones.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Qué ve Brenda si la base de datos no responde.
 *
 * <p>Neon apaga la base después de 5 minutos sin uso y tarda unos segundos
 * en despertar. El pool de conexiones ya espera hasta 30 segundos, así que casi
 * siempre esto pasa sin que se note. Si aun así no alcanza, en lugar de un
 * error aparece una pantalla amable que explica qué pasa:</p>
 * <ul>
 *   <li>Si estaba consultando algo, la página se vuelve a cargar sola.</li>
 *   <li>Si estaba guardando, se le avisa que NO se guardó nada: la transacción
 *       nunca empezó. Nunca se reenvía un formulario automáticamente.</li>
 * </ul>
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ManejadorBaseDeDatosNoDisponible {

    private static final Logger log = LoggerFactory.getLogger(ManejadorBaseDeDatosNoDisponible.class);

    @ExceptionHandler({
            CannotCreateTransactionException.class,
            DataAccessResourceFailureException.class,
            JDBCConnectionException.class
    })
    public ModelAndView baseNoDisponible(Exception error, HttpServletRequest peticion, HttpServletResponse respuesta) {
        log.warn("La base de datos no respondió a tiempo en {} {}: {}",
                peticion.getMethod(), peticion.getRequestURI(), error.getMessage());
        respuesta.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());

        ModelAndView vista = new ModelAndView("error-base-de-datos");
        vista.addObject("reintentoAutomatico", "GET".equalsIgnoreCase(peticion.getMethod()));
        return vista;
    }
}
