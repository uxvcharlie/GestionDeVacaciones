package ni.gestionvacaciones.parametro;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Lee los parámetros del sistema y los entrega ya convertidos y validados.
 *
 * <p>Nada de valores quemados en el código: si mañana la jornada pasa de 8 a 6
 * horas, se cambia en la base de datos y el sistema entero lo toma.</p>
 */
@Service
public class ServicioParametros {

    public static final String HORAS_POR_JORNADA = "horas_por_jornada";
    public static final String ZONA_HORARIA = "zona_horaria";

    private final ParametroRepositorio repositorio;

    public ServicioParametros(ParametroRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /** Horas de una jornada laboral completa. Siempre entre 1 y 24. */
    @Transactional(readOnly = true)
    public int horasPorJornada() {
        String valor = valor(HORAS_POR_JORNADA);
        try {
            int horas = Integer.parseInt(valor.trim());
            if (horas < 1 || horas > 24) {
                throw new IllegalStateException(
                        "El parámetro horas_por_jornada debe estar entre 1 y 24, y vale " + horas + ".");
            }
            return horas;
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "El parámetro horas_por_jornada no es un número entero: \"" + valor + "\".", e);
        }
    }

    /** Zona horaria con la que se muestran fechas y horas. */
    @Transactional(readOnly = true)
    public ZoneId zonaHoraria() {
        String valor = valor(ZONA_HORARIA);
        try {
            return ZoneId.of(valor.trim());
        } catch (DateTimeException e) {
            throw new IllegalStateException(
                    "El parámetro zona_horaria no es una zona válida: \"" + valor + "\".", e);
        }
    }

    private String valor(String clave) {
        return repositorio.findById(clave)
                .map(Parametro::getValor)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el parámetro " + clave + " en la base de datos."));
    }
}
