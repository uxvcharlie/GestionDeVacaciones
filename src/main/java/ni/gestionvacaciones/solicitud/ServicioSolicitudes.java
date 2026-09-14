package ni.gestionvacaciones.solicitud;

import ni.gestionvacaciones.auditoria.AccionAuditoria;
import ni.gestionvacaciones.auditoria.ServicioAuditoria;
import ni.gestionvacaciones.comun.ConversorTiempo;
import ni.gestionvacaciones.comun.ReglaDeNegocioException;
import ni.gestionvacaciones.empleado.Empleado;
import ni.gestionvacaciones.empleado.EmpleadoNoEncontradoException;
import ni.gestionvacaciones.empleado.EmpleadoRepositorio;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.saldo.CambioDeSaldo;
import ni.gestionvacaciones.saldo.ServicioSaldo;
import ni.gestionvacaciones.saldo.TipoMovimiento;
import ni.gestionvacaciones.seguridad.Rol;
import ni.gestionvacaciones.seguridad.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registrar y anular vacaciones, citas médicas y permisos.
 *
 * <h2>Las reglas, en orden</h2>
 * <ol>
 *   <li>Los tres tipos descuentan del MISMO saldo, igual.</li>
 *   <li>Al registrar se descuenta en el acto y queda el movimiento CONSUMO.</li>
 *   <li>El saldo no puede quedar negativo, salvo que un ADMIN lo autorice con
 *       una casilla explícita y un motivo. Queda en la bitácora.</li>
 *   <li>Anular no borra: cambia el estado, exige motivo y devuelve el tiempo
 *       con un movimiento REVERSION.</li>
 *   <li>Fechas al revés bloquean. Fechas a más de un año, o que se cruzan
 *       con otra solicitud, solo advierten.</li>
 * </ol>
 *
 * <p>Todas las reglas se revisan de nuevo al guardar, aunque la pantalla de
 * confirmación ya las haya revisado: entre una y otra el saldo pudo cambiar,
 * y lo que llega del navegador nunca se da por bueno.</p>
 */
@Service
public class ServicioSolicitudes {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ESPANOL = Locale.forLanguageTag("es-NI");

    private final SolicitudRepositorio solicitudes;
    private final EmpleadoRepositorio empleados;
    private final ServicioSaldo servicioSaldo;
    private final ServicioParametros parametros;
    private final ServicioAuditoria auditoria;

    public ServicioSolicitudes(SolicitudRepositorio solicitudes,
                               EmpleadoRepositorio empleados,
                               ServicioSaldo servicioSaldo,
                               ServicioParametros parametros,
                               ServicioAuditoria auditoria) {
        this.solicitudes = solicitudes;
        this.empleados = empleados;
        this.servicioSaldo = servicioSaldo;
        this.parametros = parametros;
        this.auditoria = auditoria;
    }

    /** Revisa una solicitud sin guardar nada. Para la pantalla de confirmación. */
    @Transactional(readOnly = true)
    public EvaluacionSolicitud evaluar(FormularioSolicitud formulario, Usuario usuario) {
        Empleado empleado = empleados.findById(formulario.getEmpleadoId())
                .orElseThrow(() -> new EmpleadoNoEncontradoException(formulario.getEmpleadoId()));
        return evaluar(formulario, empleado, usuario);
    }

    /**
     * Registra la solicitud y descuenta el saldo, todo o nada.
     *
     * <p>La fila del funcionario se bloquea ANTES de evaluar, así el saldo con
     * el que se decide es el mismo con el que se descuenta.</p>
     */
    @Transactional
    public ResultadoSolicitud registrar(FormularioSolicitud formulario, Usuario usuario, String ip) {
        Empleado empleado = empleados.bloquearParaCambiarSaldo(formulario.getEmpleadoId())
                .orElseThrow(() -> new EmpleadoNoEncontradoException(formulario.getEmpleadoId()));
        EvaluacionSolicitud evaluacion = evaluar(formulario, empleado, usuario);

        if (evaluacion.tieneErrores()) {
            throw new ReglaDeNegocioException(evaluacion.errores().get(0));
        }

        boolean negativoAutorizado = false;
        if (evaluacion.requiereAutorizacion()) {
            if (usuario.getRol() != Rol.ADMIN) {
                throw new ReglaDeNegocioException(evaluacion.mensajeSaldoInsuficiente()
                        + " Solo una persona administradora puede autorizar un saldo negativo.");
            }
            if (!formulario.isAutorizarSaldoNegativo()) {
                throw new ReglaDeNegocioException(evaluacion.mensajeSaldoInsuficiente(), true);
            }
            if (limpiar(formulario.getMotivoAutorizacion()) == null) {
                throw new ReglaDeNegocioException(
                        "Para dejar el saldo en negativo tenés que escribir el motivo de la autorización.", true);
            }
            negativoAutorizado = true;
        }

        Solicitud solicitud = solicitudes.save(new Solicitud(
                empleado.getId(), formulario.getTipo(), formulario.getFechaInicio(), formulario.getFechaFin(),
                evaluacion.minutos(), limpiar(formulario.getMotivo()), usuario.getId()));

        String descripcion = describir(solicitud);
        if (negativoAutorizado) {
            descripcion += " · Saldo negativo autorizado: " + limpiar(formulario.getMotivoAutorizacion());
        }

        CambioDeSaldo cambio = servicioSaldo.aplicar(empleado.getId(), TipoMovimiento.CONSUMO,
                -evaluacion.minutos(), solicitud.getId(), descripcion, usuario.getId(), negativoAutorizado);

        int jornada = parametros.horasPorJornada();
        auditoria.registrar(usuario.getId(), AccionAuditoria.SOLICITUD_REGISTRADA,
                ServicioAuditoria.ENTIDAD_SOLICITUD, solicitud.getId(),
                empleado.getNombreCompleto() + " · " + describir(solicitud) + " · "
                        + solicitud.getTipo().formatear(solicitud.getMinutosSolicitados(), jornada)
                        + " · Saldo: " + ConversorTiempo.formatear(cambio.saldoAntes(), jornada)
                        + " → " + ConversorTiempo.formatear(cambio.saldoDespues(), jornada),
                ip);

        if (negativoAutorizado) {
            auditoria.registrar(usuario.getId(), AccionAuditoria.SALDO_NEGATIVO_AUTORIZADO,
                    ServicioAuditoria.ENTIDAD_SOLICITUD, solicitud.getId(),
                    empleado.getNombreCompleto() + " quedó con un saldo de "
                            + ConversorTiempo.formatear(cambio.saldoDespues(), jornada)
                            + ". Motivo: " + limpiar(formulario.getMotivoAutorizacion()),
                    ip);
        }
        return new ResultadoSolicitud(solicitud, cambio);
    }

    /**
     * Anula una solicitud y devuelve su tiempo al saldo, todo o nada.
     * La solicitud se bloquea primero: no se puede anular dos veces.
     */
    @Transactional
    public ResultadoSolicitud anular(Long solicitudId, String motivo, Usuario usuario, String ip) {
        Solicitud solicitud = solicitudes.bloquearParaAnular(solicitudId)
                .orElseThrow(() -> new SolicitudNoEncontradaException(solicitudId));

        solicitud.anular(usuario.getId(), motivo);

        CambioDeSaldo cambio = servicioSaldo.aplicar(solicitud.getEmpleadoId(), TipoMovimiento.REVERSION,
                solicitud.getMinutosSolicitados(), solicitud.getId(),
                "Anulación: " + describir(solicitud) + " · " + solicitud.getMotivoAnulacion(),
                usuario.getId(), true);

        int jornada = parametros.horasPorJornada();
        auditoria.registrar(usuario.getId(), AccionAuditoria.SOLICITUD_ANULADA,
                ServicioAuditoria.ENTIDAD_SOLICITUD, solicitud.getId(),
                cambio.empleado().getNombreCompleto() + " · " + describir(solicitud)
                        + " · Se devolvieron " + solicitud.getTipo().formatear(solicitud.getMinutosSolicitados(), jornada)
                        + " · Motivo: " + solicitud.getMotivoAnulacion(),
                ip);

        return new ResultadoSolicitud(solicitud, cambio);
    }

    @Transactional(readOnly = true)
    public Solicitud obtener(Long id) {
        return solicitudes.findById(id).orElseThrow(() -> new SolicitudNoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public List<Solicitud> historial(Long empleadoId) {
        return solicitudes.findByEmpleadoIdOrderByFechaInicioDescIdDesc(empleadoId);
    }

    /** Cuánto se fue este año (en Managua) en vacaciones, citas y permisos. */
    @Transactional(readOnly = true)
    public DesgloseAnual desgloseDelAnio(Long empleadoId) {
        int anio = LocalDate.now(parametros.zonaHoraria()).getYear();
        Map<TipoSolicitud, Integer> minutos = new EnumMap<>(TipoSolicitud.class);
        for (Object[] fila : solicitudes.sumarMinutosPorTipo(empleadoId, EstadoSolicitud.REGISTRADA,
                LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31))) {
            minutos.put((TipoSolicitud) fila[0], ((Number) fila[1]).intValue());
        }
        return new DesgloseAnual(anio, minutos);
    }

    /** "Vacaciones del 10/11/2026 al 14/11/2026" o "Cita médica el 21/09/2026". */
    public static String describir(Solicitud solicitud) {
        return solicitud.getTipo().getEtiqueta() + " " + rango(solicitud.getFechaInicio(), solicitud.getFechaFin());
    }

    // -------------------------------------------------------------------------

    private EvaluacionSolicitud evaluar(FormularioSolicitud formulario, Empleado empleado, Usuario usuario) {
        if (formulario.getTipo() == null || formulario.getFechaInicio() == null || formulario.getFechaFin() == null) {
            throw new ReglaDeNegocioException("Faltan datos de la solicitud: el tipo y las dos fechas.");
        }
        int jornada = parametros.horasPorJornada();
        LocalDate hoy = LocalDate.now(parametros.zonaHoraria());
        LocalDate inicio = formulario.getFechaInicio();
        LocalDate fin = formulario.getFechaFin();
        TipoSolicitud tipo = formulario.getTipo();

        List<String> errores = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();

        if (!empleado.isActivo()) {
            errores.add(empleado.getNombreCompleto() + " está de baja: no se le pueden registrar solicitudes.");
        }
        boolean fechasAlReves = fin.isBefore(inicio);
        if (fechasAlReves) {
            errores.add("La fecha final no puede ser anterior a la fecha de inicio.");
        }
        int minutos = formulario.minutosTotales(jornada);
        if (minutos <= 0) {
            errores.add(tipo.isSeCuentaEnDias()
                    ? "Escribí cuántos días u horas son. No puede ser cero."
                    : "Escribí cuántas horas o minutos son. No puede ser cero.");
        }

        if (inicio.isBefore(hoy.minusYears(1))) {
            advertencias.add("La fecha de inicio es de hace más de un año. Revisá que esté bien escrita.");
        }
        if (fin.isAfter(hoy.plusYears(1))) {
            advertencias.add("La fecha final es dentro de más de un año. Revisá que esté bien escrita.");
        }
        if (!fechasAlReves) {
            for (Solicitud otra : solicitudes.buscarTraslapes(empleado.getId(), EstadoSolicitud.REGISTRADA, inicio, fin)) {
                advertencias.add("Ya hay una solicitud de " + otra.getTipo().getEtiqueta().toLowerCase(ESPANOL)
                        + " " + rango(otra.getFechaInicio(), otra.getFechaFin())
                        + " que se cruza con estas fechas. Revisá que no sea la misma.");
            }
        }

        int saldoAntes = empleado.getSaldoVacacionesMinutos();
        int saldoDespues = saldoAntes - Math.max(minutos, 0);
        String mensajeSaldo = null;
        if (errores.isEmpty() && saldoDespues < 0) {
            mensajeSaldo = usuario.primerNombre() + ", " + empleado.getNombreCompleto() + " solo tiene "
                    + ConversorTiempo.formatear(saldoAntes, jornada) + " de saldo y estás registrando "
                    + tipo.formatear(minutos, jornada) + ".";
        }

        long diasCorridos = fechasAlReves ? 0 : ChronoUnit.DAYS.between(inicio, fin) + 1;

        return new EvaluacionSolicitud(empleado, tipo, inicio, fin, minutos, saldoAntes, saldoDespues,
                diasCorridos, List.copyOf(errores), List.copyOf(advertencias), mensajeSaldo);
    }

    private static String rango(LocalDate inicio, LocalDate fin) {
        return inicio.equals(fin)
                ? "el " + inicio.format(FECHA)
                : "del " + inicio.format(FECHA) + " al " + fin.format(FECHA);
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
