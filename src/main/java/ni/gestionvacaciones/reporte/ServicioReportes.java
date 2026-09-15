package ni.gestionvacaciones.reporte;

import ni.gestionvacaciones.auditoria.AccionAuditoria;
import ni.gestionvacaciones.auditoria.ServicioAuditoria;
import ni.gestionvacaciones.comun.ConversorTiempo;
import ni.gestionvacaciones.comun.Formatos;
import ni.gestionvacaciones.empleado.Empleado;
import ni.gestionvacaciones.empleado.ServicioEmpleados;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.saldo.MovimientoSaldo;
import ni.gestionvacaciones.saldo.ServicioSaldo;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import ni.gestionvacaciones.solicitud.DesgloseAnual;
import ni.gestionvacaciones.solicitud.EstadoSolicitud;
import ni.gestionvacaciones.solicitud.ServicioSolicitudes;
import ni.gestionvacaciones.solicitud.Solicitud;
import ni.gestionvacaciones.solicitud.SolicitudRepositorio;
import ni.gestionvacaciones.solicitud.TipoSolicitud;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Los reportes para imprimir o guardar: el de un funcionario y el de un mes.
 *
 * <p>El tiempo va siempre en dos columnas: en texto ("5 días", "2 horas y 30
 * minutos") para leer, y en minutos enteros para sumar en Excel. Nunca en días
 * con decimales.</p>
 *
 * <p>Cada descarga queda en la bitácora: los reportes llevan datos personales.</p>
 */
@Service
public class ServicioReportes {

    public enum Formato {
        EXCEL, CSV;

        /** "excel" o "csv", como llegan en la dirección. */
        public static Formato desde(String texto) {
            return switch (texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT)) {
                case "excel", "xlsx" -> EXCEL;
                case "csv" -> CSV;
                default -> throw new IllegalArgumentException("Formato de reporte desconocido: " + texto);
            };
        }
    }

    /** Un archivo listo para descargar. */
    public record Archivo(String nombre, String tipoContenido, byte[] contenido) {
    }

    private static final Locale ESPANOL = Locale.forLanguageTag("es-NI");
    private static final String TIPO_CSV = "text/csv; charset=UTF-8";

    private final ServicioEmpleados servicioEmpleados;
    private final ServicioSolicitudes servicioSolicitudes;
    private final ServicioSaldo servicioSaldo;
    private final SolicitudRepositorio solicitudes;
    private final ServicioParametros parametros;
    private final ServicioUsuario servicioUsuario;
    private final ServicioAuditoria auditoria;
    private final Formatos formatos;
    private final Clock reloj;

    public ServicioReportes(ServicioEmpleados servicioEmpleados,
                            ServicioSolicitudes servicioSolicitudes,
                            ServicioSaldo servicioSaldo,
                            SolicitudRepositorio solicitudes,
                            ServicioParametros parametros,
                            ServicioUsuario servicioUsuario,
                            ServicioAuditoria auditoria,
                            Formatos formatos,
                            Clock reloj) {
        this.servicioEmpleados = servicioEmpleados;
        this.servicioSolicitudes = servicioSolicitudes;
        this.servicioSaldo = servicioSaldo;
        this.solicitudes = solicitudes;
        this.parametros = parametros;
        this.servicioUsuario = servicioUsuario;
        this.auditoria = auditoria;
        this.formatos = formatos;
        this.reloj = reloj;
    }

    /**
     * Reporte de un funcionario.
     * Excel: hojas Resumen, Solicitudes y Movimientos. CSV: la tabla de solicitudes.
     */
    @Transactional
    public Archivo funcionario(Long empleadoId, Formato formato, Long usuarioId, String ip) {
        Empleado empleado = servicioEmpleados.obtener(empleadoId);
        List<Solicitud> historial = servicioSolicitudes.historial(empleadoId);
        List<MovimientoSaldo> movimientos = servicioSaldo.historial(empleadoId);
        int jornada = parametros.horasPorJornada();
        ZoneId zona = parametros.zonaHoraria();

        Set<Long> ids = new HashSet<>();
        historial.forEach(s -> {
            ids.add(s.getRegistradoPor());
            if (s.getAnuladoPor() != null) {
                ids.add(s.getAnuladoPor());
            }
        });
        movimientos.forEach(m -> ids.add(m.getRealizadoPor()));
        Map<Long, String> nombres = servicioUsuario.nombresPorId(ids);

        List<List<Object>> filasSolicitudes = new ArrayList<>();
        for (Solicitud s : historial) {
            filasSolicitudes.add(new ArrayList<>(celdasDeSolicitud(s, nombres, jornada, zona)));
        }
        Tabla tablaSolicitudes = new Tabla("Solicitudes", ENCABEZADOS_SOLICITUD, filasSolicitudes);

        String base = "funcionario-" + nombreDeArchivo(empleado.getNombreCompleto()) + "-" + hoy(zona);
        Archivo archivo;
        if (formato == Formato.CSV) {
            archivo = new Archivo(base + ".csv", TIPO_CSV, EscritorCsv.escribir(tablaSolicitudes));
        } else {
            archivo = new Archivo(base + ".xlsx", EscritorExcel.TIPO_CONTENIDO, EscritorExcel.escribir(List.of(
                    resumenDeFuncionario(empleado, jornada, zona),
                    tablaSolicitudes,
                    tablaMovimientos(movimientos, nombres, jornada, zona))));
        }

        auditoria.registrar(usuarioId, AccionAuditoria.REPORTE_DESCARGADO, ServicioAuditoria.ENTIDAD_REPORTE, empleadoId,
                "Reporte de " + empleado.getNombreCompleto() + " (" + (formato == Formato.CSV ? "CSV" : "Excel") + ")", ip);
        return archivo;
    }

    /**
     * Reporte del mes: todas las solicitudes que empiezan en ese mes, también las
     * anuladas (marcadas). Excel: hojas Resumen y Solicitudes. CSV: solicitudes.
     */
    @Transactional
    public Archivo mes(YearMonth mes, Formato formato, Long usuarioId, String ip) {
        int jornada = parametros.horasPorJornada();
        ZoneId zona = parametros.zonaHoraria();
        List<SolicitudConFuncionario> filas = solicitudes.conFuncionarioEnPeriodo(mes.atDay(1), mes.atEndOfMonth());

        Set<Long> ids = new HashSet<>();
        filas.forEach(f -> {
            ids.add(f.solicitud().getRegistradoPor());
            if (f.solicitud().getAnuladoPor() != null) {
                ids.add(f.solicitud().getAnuladoPor());
            }
        });
        Map<Long, String> nombres = servicioUsuario.nombresPorId(ids);

        List<String> encabezados = new ArrayList<>(List.of("Funcionario", "Cargo", "Área o dependencia"));
        encabezados.addAll(ENCABEZADOS_SOLICITUD);
        List<List<Object>> celdas = new ArrayList<>();
        for (SolicitudConFuncionario f : filas) {
            List<Object> fila = new ArrayList<>();
            fila.add(f.nombre());
            fila.add(f.cargo());
            fila.add(f.area());
            fila.addAll(celdasDeSolicitud(f.solicitud(), nombres, jornada, zona));
            celdas.add(fila);
        }
        Tabla tablaSolicitudes = new Tabla("Solicitudes", encabezados, celdas);

        String nombreMes = nombreDelMes(mes);
        String base = "solicitudes-" + mes;
        Archivo archivo;
        if (formato == Formato.CSV) {
            archivo = new Archivo(base + ".csv", TIPO_CSV, EscritorCsv.escribir(tablaSolicitudes));
        } else {
            archivo = new Archivo(base + ".xlsx", EscritorExcel.TIPO_CONTENIDO, EscritorExcel.escribir(List.of(
                    resumenDelMes(nombreMes, filas, jornada, zona), tablaSolicitudes)));
        }

        auditoria.registrar(usuarioId, AccionAuditoria.REPORTE_DESCARGADO, ServicioAuditoria.ENTIDAD_REPORTE, null,
                "Solicitudes de " + nombreMes + " (" + (formato == Formato.CSV ? "CSV" : "Excel") + ")", ip);
        return archivo;
    }

    /** "septiembre de 2026" */
    public static String nombreDelMes(YearMonth mes) {
        return mes.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, ESPANOL) + " de " + mes.getYear();
    }

    // -------------------------------------------------------------------------

    private static final List<String> ENCABEZADOS_SOLICITUD = List.of(
            "Tipo", "Desde", "Hasta", "Tiempo", "Minutos", "Estado", "Motivo",
            "Registrada por", "Registrada el", "Anulada por", "Anulada el", "Motivo de la anulación");

    private List<Object> celdasDeSolicitud(Solicitud s, Map<Long, String> nombres, int jornada, ZoneId zona) {
        List<Object> celdas = new ArrayList<>();
        celdas.add(s.getTipo().getEtiqueta());
        celdas.add(s.getFechaInicio());
        celdas.add(s.getFechaFin());
        celdas.add(s.getTipo().formatear(s.getMinutosSolicitados(), jornada));
        celdas.add(s.getMinutosSolicitados());
        celdas.add(s.getEstado().getEtiqueta());
        celdas.add(s.getMotivo());
        celdas.add(nombres.get(s.getRegistradoPor()));
        celdas.add(formatos.fechaHora(s.getCreadoEn(), zona));
        celdas.add(s.getAnuladoPor() == null ? null : nombres.get(s.getAnuladoPor()));
        celdas.add(s.getAnuladoEn() == null ? null : formatos.fechaHora(s.getAnuladoEn(), zona));
        celdas.add(s.getMotivoAnulacion());
        return celdas;
    }

    private Tabla resumenDeFuncionario(Empleado empleado, int jornada, ZoneId zona) {
        DesgloseAnual desglose = servicioSolicitudes.desgloseDelAnio(empleado.getId());
        List<List<Object>> filas = new ArrayList<>();
        filas.add(par("Funcionario", empleado.getNombreCompleto()));
        filas.add(par("Cargo", empleado.getCargo()));
        filas.add(par("Área o dependencia", empleado.getAreaODependencia()));
        filas.add(par("Fecha de ingreso", empleado.getFechaIngreso()));
        filas.add(par("Estado", empleado.isActivo() ? "Activo" : "De baja"));
        filas.add(par("Saldo disponible", ConversorTiempo.formatear(empleado.getSaldoVacacionesMinutos(), jornada)));
        filas.add(par("Saldo en minutos", empleado.getSaldoVacacionesMinutos()));
        filas.add(par("Jornada", jornada + " horas"));
        for (TipoSolicitud tipo : TipoSolicitud.values()) {
            filas.add(par(tipo.getEtiqueta() + " en " + desglose.anio(),
                    tipo.formatear(desglose.minutos(tipo), jornada)));
        }
        filas.add(par("Reporte generado el", formatos.fechaHora(Instant.now(reloj), zona)));
        return new Tabla("Resumen", List.of("Dato", "Valor"), filas);
    }

    private Tabla tablaMovimientos(List<MovimientoSaldo> movimientos, Map<Long, String> nombres, int jornada, ZoneId zona) {
        List<List<Object>> filas = new ArrayList<>();
        for (MovimientoSaldo m : movimientos) {
            List<Object> fila = new ArrayList<>();
            fila.add(formatos.fechaHora(m.getCreadoEn(), zona));
            fila.add(m.getTipoMovimiento().getEtiqueta());
            fila.add(formatos.tiempoConSigno(m.getMinutos(), jornada));
            fila.add(m.getMinutos());
            fila.add(ConversorTiempo.formatear(m.getSaldoResultanteMinutos(), jornada));
            fila.add(m.getSaldoResultanteMinutos());
            fila.add(m.getDescripcion());
            fila.add(nombres.get(m.getRealizadoPor()));
            filas.add(fila);
        }
        return new Tabla("Movimientos", List.of("Fecha y hora", "Movimiento", "Tiempo", "Minutos",
                "Saldo después", "Saldo después (minutos)", "Descripción", "Realizado por"), filas);
    }

    private Tabla resumenDelMes(String nombreMes, List<SolicitudConFuncionario> filas, int jornada, ZoneId zona) {
        List<List<Object>> resumen = new ArrayList<>();
        resumen.add(par("Mes", nombreMes));
        long totalCantidad = 0;
        int totalMinutos = 0;
        for (TipoSolicitud tipo : TipoSolicitud.values()) {
            long cantidad = 0;
            int minutos = 0;
            for (SolicitudConFuncionario f : filas) {
                if (f.solicitud().getTipo() == tipo && f.solicitud().getEstado() == EstadoSolicitud.REGISTRADA) {
                    cantidad++;
                    minutos += f.solicitud().getMinutosSolicitados();
                }
            }
            totalCantidad += cantidad;
            totalMinutos += minutos;
            resumen.add(List.of(tipo.getEtiqueta(), cantidad + (cantidad == 1 ? " solicitud" : " solicitudes")
                    + " · " + tipo.formatear(minutos, jornada), minutos));
        }
        resumen.add(List.of("Total", totalCantidad + (totalCantidad == 1 ? " solicitud" : " solicitudes")
                + " · " + ConversorTiempo.formatear(totalMinutos, jornada), totalMinutos));
        resumen.add(par("Anuladas (no se suman)",
                filas.stream().filter(f -> f.solicitud().getEstado() == EstadoSolicitud.ANULADA).count()));
        resumen.add(par("Reporte generado el", formatos.fechaHora(Instant.now(reloj), zona)));
        return new Tabla("Resumen", List.of("Dato", "Valor", "Minutos"), resumen);
    }

    private static List<Object> par(String dato, Object valor) {
        List<Object> fila = new ArrayList<>();
        fila.add(dato);
        fila.add(valor);
        return fila;
    }

    private LocalDate hoy(ZoneId zona) {
        return LocalDate.now(reloj.withZone(zona));
    }

    /** "Ángela Pérez Núñez" → "angela-perez-nunez" */
    static String nombreDeArchivo(String nombre) {
        String sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String limpio = sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return limpio.isEmpty() ? "sin-nombre" : limpio;
    }
}
