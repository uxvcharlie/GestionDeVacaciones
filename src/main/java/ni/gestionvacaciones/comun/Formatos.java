package ni.gestionvacaciones.comun;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Formatos de fecha, hora y tiempo para las pantallas.
 *
 * <p>Es un componente de Spring con nombre "formatos" para poder usarlo desde
 * las plantillas así: {@code ${@formatos.fecha(empleado.fechaIngreso)}}.</p>
 *
 * <p>La zona horaria y la jornada se reciben como argumento (el controlador las
 * lee una vez por pantalla) para no consultar la base de datos por cada fila.</p>
 */
@Component("formatos")
public class Formatos {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("h:mm");

    /** 21/09/2026 */
    public String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    /**
     * 21/09/2026 3:45 pm, en la zona indicada.
     *
     * <p>El "am/pm" se arma a mano: el formato de Java para español cambia entre
     * versiones ("p. m.", "p.m.") y queremos que se vea siempre igual.</p>
     */
    public String fechaHora(Instant instante, ZoneId zona) {
        if (instante == null) {
            return "";
        }
        ZonedDateTime local = instante.atZone(zona);
        return local.format(FECHA) + " " + local.format(HORA) + (local.getHour() < 12 ? " am" : " pm");
    }

    /** "8 días y 6 horas". Ver {@link ConversorTiempo#formatear}. */
    public String tiempo(int minutos, int horasPorJornada) {
        return ConversorTiempo.formatear(minutos, horasPorJornada);
    }

    /** Igual que {@link #tiempo}, pero con "+" adelante si suma: "+10 días", "−1 día". */
    public String tiempoConSigno(int minutos, int horasPorJornada) {
        String texto = ConversorTiempo.formatear(minutos, horasPorJornada);
        return minutos > 0 ? "+" + texto : texto;
    }
}
