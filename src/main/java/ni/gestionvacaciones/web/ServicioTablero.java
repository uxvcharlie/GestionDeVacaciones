package ni.gestionvacaciones.web;

import ni.gestionvacaciones.empleado.EmpleadoRepositorio;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.solicitud.EstadoSolicitud;
import ni.gestionvacaciones.solicitud.SolicitudRepositorio;
import ni.gestionvacaciones.solicitud.TipoSolicitud;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Arma el resumen del tablero: saludo, funcionarios activos, solicitudes del
 * mes por tipo y las últimas registradas.
 *
 * <p>"Del mes" significa: solicitudes registradas (no anuladas) cuya fecha de
 * inicio cae en el mes actual, en la hora de Managua. Es el mismo criterio que
 * el desglose del año en la ficha.</p>
 */
@Service
public class ServicioTablero {

    private static final Locale ESPANOL = Locale.forLanguageTag("es-NI");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ESPANOL);
    private static final int ULTIMAS = 5;

    private final EmpleadoRepositorio empleados;
    private final SolicitudRepositorio solicitudes;
    private final ServicioParametros parametros;
    private final Clock reloj;

    public ServicioTablero(EmpleadoRepositorio empleados,
                           SolicitudRepositorio solicitudes,
                           ServicioParametros parametros,
                           Clock reloj) {
        this.empleados = empleados;
        this.solicitudes = solicitudes;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public ResumenTablero resumen(Usuario usuario) {
        ZonedDateTime ahora = ZonedDateTime.now(reloj).withZoneSameInstant(parametros.zonaHoraria());
        LocalDate hoy = ahora.toLocalDate();
        LocalDate primerDia = hoy.withDayOfMonth(1);
        LocalDate ultimoDia = hoy.withDayOfMonth(hoy.lengthOfMonth());

        Map<TipoSolicitud, ResumenTablero.ResumenTipo> delMes = new EnumMap<>(TipoSolicitud.class);
        for (TipoSolicitud tipo : TipoSolicitud.values()) {
            delMes.put(tipo, ResumenTablero.ResumenTipo.VACIO);
        }
        for (Object[] fila : solicitudes.resumirPorTipo(EstadoSolicitud.REGISTRADA, primerDia, ultimoDia)) {
            delMes.put((TipoSolicitud) fila[0],
                    new ResumenTablero.ResumenTipo(((Number) fila[1]).longValue(), ((Number) fila[2]).intValue()));
        }

        String mes = primerDia.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, ESPANOL)
                + " de " + primerDia.getYear();

        return new ResumenTablero(
                Saludo.para(ahora.toLocalTime()) + ", " + usuario.primerNombre(),
                hoy.format(FECHA_LARGA),
                mes,
                empleados.countByActivoTrue(),
                empleados.count() > 0,
                delMes,
                solicitudes.recientes(Limit.of(ULTIMAS)));
    }
}
