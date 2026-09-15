package ni.gestionvacaciones.solicitud;

import ni.gestionvacaciones.PruebaConPostgres;
import ni.gestionvacaciones.auditoria.Auditoria;
import ni.gestionvacaciones.auditoria.AuditoriaRepositorio;
import ni.gestionvacaciones.auditoria.ServicioAuditoria;
import ni.gestionvacaciones.comun.ConversorTiempo;
import ni.gestionvacaciones.comun.ReglaDeNegocioException;
import ni.gestionvacaciones.empleado.Empleado;
import ni.gestionvacaciones.empleado.EmpleadoRepositorio;
import ni.gestionvacaciones.empleado.FormularioEmpleado;
import ni.gestionvacaciones.empleado.ServicioEmpleados;
import ni.gestionvacaciones.saldo.MovimientoSaldo;
import ni.gestionvacaciones.saldo.ServicioSaldo;
import ni.gestionvacaciones.saldo.TipoMovimiento;
import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.seguridad.UsuarioRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Las reglas de la Fase 3 contra un PostgreSQL real. Es la parte donde un
 * error hace daño de verdad: un saldo mal calculado es un reclamo laboral.
 *
 * <p>Cada prueba usa su propio funcionario, así no dependen unas de otras.</p>
 */
class ServicioSolicitudesTest extends PruebaConPostgres {

    private static final int JORNADA = 8;
    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Managua"));

    @Autowired
    private ServicioSolicitudes servicioSolicitudes;

    @Autowired
    private ServicioEmpleados servicioEmpleados;

    @Autowired
    private ServicioSaldo servicioSaldo;

    @Autowired
    private EmpleadoRepositorio empleadoRepositorio;

    @Autowired
    private SolicitudRepositorio solicitudRepositorio;

    @Autowired
    private AuditoriaRepositorio auditoriaRepositorio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    // ---------------------------------------------------------------------
    // El ejemplo de la usuaria
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("El ejemplo de Brenda: 10 días → 1 día de vacaciones → 9 días → cita de 2 h → 8 días y 6 horas")
    void ejemploDeBrenda() {
        Empleado funcionario = crearFuncionario("Solicitud Ejemplo Brenda", 10, 0);

        servicioSolicitudes.registrar(vacaciones(funcionario, HOY, HOY, 1, 0), brenda(), null);
        assertThat(saldoTexto(funcionario)).isEqualTo("9 días");

        servicioSolicitudes.registrar(conHoras(funcionario, TipoSolicitud.CITA_MEDICA, HOY.plusDays(1), 2, 0), brenda(), null);
        assertThat(saldo(funcionario)).isEqualTo(4200);
        assertThat(saldoTexto(funcionario)).isEqualTo("8 días y 6 horas");

        List<MovimientoSaldo> historial = servicioSaldo.historial(funcionario.getId());
        assertThat(historial).extracting(MovimientoSaldo::getTipoMovimiento)
                .containsExactly(TipoMovimiento.CONSUMO, TipoMovimiento.CONSUMO, TipoMovimiento.SALDO_INICIAL);
        assertThat(historial).extracting(MovimientoSaldo::getMinutos).containsExactly(-120, -480, 4800);
        assertThat(historial).extracting(MovimientoSaldo::getSaldoResultanteMinutos).containsExactly(4200, 4320, 4800);
    }

    @Test
    @DisplayName("Los tres tipos descuentan del mismo saldo, y el desglose del año los separa")
    void losTresTiposDescuentanDelMismoSaldo() {
        Empleado funcionario = crearFuncionario("Solicitud Tres Tipos", 5, 0);

        servicioSolicitudes.registrar(vacaciones(funcionario, HOY, HOY, 1, 0), brenda(), null);
        servicioSolicitudes.registrar(conHoras(funcionario, TipoSolicitud.CITA_MEDICA, HOY.plusDays(2), 3, 0), brenda(), null);
        servicioSolicitudes.registrar(conHoras(funcionario, TipoSolicitud.PERMISO, HOY.plusDays(3), 1, 30), brenda(), null);

        assertThat(saldo(funcionario)).isEqualTo(2400 - 480 - 180 - 90);

        DesgloseAnual desglose = servicioSolicitudes.desgloseDelAnio(funcionario.getId());
        // Las fechas de prueba pueden cruzar a otro año si hoy es fin de diciembre.
        if (HOY.plusDays(3).getYear() == HOY.getYear()) {
            assertThat(desglose.minutos(TipoSolicitud.VACACIONES)).isEqualTo(480);
            assertThat(desglose.minutos(TipoSolicitud.CITA_MEDICA)).isEqualTo(180);
            assertThat(desglose.minutos(TipoSolicitud.PERMISO)).isEqualTo(90);
        }
    }

    @Test
    @DisplayName("Medio día de vacaciones descuenta 4 horas, no un día completo")
    void medioDiaDeVacaciones() {
        Empleado funcionario = crearFuncionario("Solicitud Medio Día", 2, 0);

        servicioSolicitudes.registrar(vacaciones(funcionario, HOY, HOY, 0, 4), brenda(), null);

        assertThat(saldoTexto(funcionario)).isEqualTo("1 día y 4 horas");
    }

    // ---------------------------------------------------------------------
    // Saldo negativo
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Si el saldo no alcanza se bloquea, con el mensaje claro, y no se guarda nada")
    void saldoInsuficienteBloquea() {
        Empleado funcionario = crearFuncionario("María Saldo Corto", 3, 0);

        assertThatThrownBy(() -> servicioSolicitudes.registrar(
                vacaciones(funcionario, HOY, HOY.plusDays(4), 5, 0), brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Brenda, María Saldo Corto solo tiene 3 días de saldo y estás registrando 5 días.")
                .satisfies(e -> assertThat(((ReglaDeNegocioException) e).requiereAutorizacion()).isTrue());

        assertThat(saldo(funcionario)).isEqualTo(1440);
        assertThat(solicitudRepositorio.findByEmpleadoIdOrderByFechaInicioDescIdDesc(funcionario.getId())).isEmpty();
        assertThat(servicioSaldo.historial(funcionario.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Marcar la autorización sin escribir el motivo también bloquea")
    void autorizacionSinMotivoBloquea() {
        Empleado funcionario = crearFuncionario("Solicitud Sin Motivo De Excepción", 1, 0);
        FormularioSolicitud formulario = vacaciones(funcionario, HOY, HOY.plusDays(1), 2, 0);
        formulario.setAutorizarSaldoNegativo(true);
        formulario.setMotivoAutorizacion("   ");

        assertThatThrownBy(() -> servicioSolicitudes.registrar(formulario, brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("motivo de la autorización");

        assertThat(saldo(funcionario)).isEqualTo(480);
    }

    @Test
    @DisplayName("Con autorización y motivo se permite el negativo, y queda en la bitácora")
    void excepcionAutorizadaQuedaEnBitacora() {
        Empleado funcionario = crearFuncionario("Solicitud Excepción Autorizada", 3, 0);
        FormularioSolicitud formulario = vacaciones(funcionario, HOY, HOY.plusDays(4), 5, 0);
        formulario.setAutorizarSaldoNegativo(true);
        formulario.setMotivoAutorizacion("Autorizado por la dirección, memo 45");

        ResultadoSolicitud resultado = servicioSolicitudes.registrar(formulario, brenda(), "10.0.0.7");

        assertThat(saldo(funcionario)).isEqualTo(-960);
        assertThat(saldoTexto(funcionario)).isEqualTo("−2 días");

        List<Auditoria> bitacora = auditoriaRepositorio.findByEntidadAndEntidadIdOrderByCreadoEnDescIdDesc(
                ServicioAuditoria.ENTIDAD_SOLICITUD, resultado.solicitud().getId());
        assertThat(bitacora).extracting(Auditoria::getAccion)
                .containsExactlyInAnyOrder("SOLICITUD_REGISTRADA", "SALDO_NEGATIVO_AUTORIZADO");
        assertThat(bitacora).allSatisfy(renglon -> {
            assertThat(renglon.getUsuarioId()).isEqualTo(brenda().getId());
            assertThat(renglon.getIp()).isEqualTo("10.0.0.7");
        });
        assertThat(bitacora).anySatisfy(renglon ->
                assertThat(renglon.getDetalle()).contains("memo 45"));
    }

    @Test
    @DisplayName("Dos registros al mismo tiempo no pueden gastar dos veces el mismo saldo")
    void registrosSimultaneos() throws Exception {
        Empleado funcionario = crearFuncionario("Solicitud Simultánea", 5, 0);
        CountDownLatch largada = new CountDownLatch(1);

        Callable<Boolean> registrarCuatroDias = () -> {
            largada.await();
            try {
                servicioSolicitudes.registrar(vacaciones(funcionario, HOY, HOY.plusDays(3), 4, 0), brenda(), null);
                return true;
            } catch (ReglaDeNegocioException e) {
                return false;
            }
        };

        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> primero = hilos.submit(registrarCuatroDias);
            Future<Boolean> segundo = hilos.submit(registrarCuatroDias);
            largada.countDown();

            List<Boolean> resultados = List.of(primero.get(30, TimeUnit.SECONDS), segundo.get(30, TimeUnit.SECONDS));
            assertThat(resultados).containsExactlyInAnyOrder(true, false);
        } finally {
            hilos.shutdownNow();
        }

        assertThat(saldoTexto(funcionario)).isEqualTo("1 día");
        assertThat(solicitudRepositorio.findByEmpleadoIdOrderByFechaInicioDescIdDesc(funcionario.getId())).hasSize(1);
    }

    // ---------------------------------------------------------------------
    // Anulación
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Anular no borra: marca ANULADA, guarda el motivo y devuelve el tiempo con REVERSION")
    void anularDevuelveElTiempo() {
        Empleado funcionario = crearFuncionario("Solicitud Para Anular", 10, 0);
        Solicitud solicitud = servicioSolicitudes.registrar(
                vacaciones(funcionario, HOY, HOY.plusDays(4), 5, 0), brenda(), null).solicitud();
        assertThat(saldoTexto(funcionario)).isEqualTo("5 días");

        servicioSolicitudes.anular(solicitud.getId(), "Se registró por error", brenda(), null);

        assertThat(saldoTexto(funcionario)).isEqualTo("10 días");
        Solicitud anulada = solicitudRepositorio.findById(solicitud.getId()).orElseThrow();
        assertThat(anulada.getEstado()).isEqualTo(EstadoSolicitud.ANULADA);
        assertThat(anulada.getMotivoAnulacion()).isEqualTo("Se registró por error");
        assertThat(anulada.getAnuladoPor()).isEqualTo(brenda().getId());
        assertThat(anulada.getAnuladoEn()).isNotNull();

        MovimientoSaldo ultimo = servicioSaldo.historial(funcionario.getId()).get(0);
        assertThat(ultimo.getTipoMovimiento()).isEqualTo(TipoMovimiento.REVERSION);
        assertThat(ultimo.getMinutos()).isEqualTo(2400);
        assertThat(ultimo.getSaldoResultanteMinutos()).isEqualTo(4800);

        assertThat(auditoriaRepositorio.findByEntidadAndEntidadIdOrderByCreadoEnDescIdDesc(
                ServicioAuditoria.ENTIDAD_SOLICITUD, solicitud.getId()))
                .extracting(Auditoria::getAccion).contains("SOLICITUD_ANULADA");
    }

    @Test
    @DisplayName("No se anula sin motivo, ni dos veces: el tiempo se devuelve una sola vez")
    void anularExigeMotivoYNoSeRepite() {
        Empleado funcionario = crearFuncionario("Solicitud Anulación Doble", 4, 0);
        Solicitud solicitud = servicioSolicitudes.registrar(
                vacaciones(funcionario, HOY, HOY, 1, 0), brenda(), null).solicitud();

        assertThatThrownBy(() -> servicioSolicitudes.anular(solicitud.getId(), " ", brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Escribí el motivo de la anulación.");
        assertThat(saldoTexto(funcionario)).isEqualTo("3 días");

        servicioSolicitudes.anular(solicitud.getId(), "Error de fecha", brenda(), null);
        assertThatThrownBy(() -> servicioSolicitudes.anular(solicitud.getId(), "Otra vez", brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class);

        assertThat(saldoTexto(funcionario)).isEqualTo("4 días");
    }

    // ---------------------------------------------------------------------
    // Validaciones de fechas y cantidad
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Fecha final anterior a la inicial bloquea")
    void fechasAlReves() {
        Empleado funcionario = crearFuncionario("Solicitud Fechas Al Revés", 5, 0);
        FormularioSolicitud formulario = vacaciones(funcionario, HOY.plusDays(3), HOY, 1, 0);

        assertThat(servicioSolicitudes.evaluar(formulario, brenda()).errores())
                .contains("La fecha final no puede ser anterior a la fecha de inicio.");
        assertThatThrownBy(() -> servicioSolicitudes.registrar(formulario, brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThat(saldoTexto(funcionario)).isEqualTo("5 días");
    }

    @Test
    @DisplayName("Una cantidad de cero bloquea")
    void cantidadCero() {
        Empleado funcionario = crearFuncionario("Solicitud Cantidad Cero", 5, 0);

        assertThatThrownBy(() -> servicioSolicitudes.registrar(
                conHoras(funcionario, TipoSolicitud.PERMISO, HOY, 0, 0), brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("No puede ser cero");
    }

    @Test
    @DisplayName("Fechas a más de un año y traslapes solo advierten: se puede guardar igual")
    void advertenciasNoBloquean() {
        Empleado funcionario = crearFuncionario("Solicitud Con Advertencias", 30, 0);
        servicioSolicitudes.registrar(vacaciones(funcionario, HOY, HOY.plusDays(2), 3, 0), brenda(), null);

        FormularioSolicitud traslapada = conHoras(funcionario, TipoSolicitud.PERMISO, HOY.plusDays(1), 2, 0);
        EvaluacionSolicitud evaluacion = servicioSolicitudes.evaluar(traslapada, brenda());
        assertThat(evaluacion.tieneErrores()).isFalse();
        assertThat(evaluacion.advertencias()).anySatisfy(a -> assertThat(a).contains("se cruza con estas fechas"));
        servicioSolicitudes.registrar(traslapada, brenda(), null);

        FormularioSolicitud lejana = vacaciones(funcionario, HOY.plusYears(1).plusDays(10), HOY.plusYears(1).plusDays(10), 1, 0);
        assertThat(servicioSolicitudes.evaluar(lejana, brenda()).advertencias())
                .anySatisfy(a -> assertThat(a).contains("más de un año"));
        servicioSolicitudes.registrar(lejana, brenda(), null);

        assertThat(solicitudRepositorio.findByEmpleadoIdOrderByFechaInicioDescIdDesc(funcionario.getId())).hasSize(3);
    }

    @Test
    @DisplayName("A un funcionario de baja no se le registran solicitudes")
    void funcionarioDeBaja() {
        Empleado funcionario = crearFuncionario("Solicitud Funcionario De Baja", 5, 0);
        servicioEmpleados.darDeBaja(funcionario.getId(), brenda().getId(), null);

        assertThatThrownBy(() -> servicioSolicitudes.registrar(
                vacaciones(funcionario, HOY, HOY, 1, 0), brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("está de baja");
    }

    @Test
    @DisplayName("El libro cuadra: la suma de los movimientos es exactamente el saldo")
    void elLibroCuadra() {
        Empleado funcionario = crearFuncionario("Solicitud Libro Contable", 7, 3);
        Solicitud primera = servicioSolicitudes.registrar(vacaciones(funcionario, HOY, HOY, 2, 0), brenda(), null).solicitud();
        servicioSolicitudes.registrar(conHoras(funcionario, TipoSolicitud.CITA_MEDICA, HOY.plusDays(5), 1, 45), brenda(), null);
        servicioSolicitudes.anular(primera.getId(), "Prueba", brenda(), null);
        servicioSolicitudes.registrar(conHoras(funcionario, TipoSolicitud.PERMISO, HOY.plusDays(6), 0, 20), brenda(), null);

        List<MovimientoSaldo> historial = servicioSaldo.historial(funcionario.getId());
        int suma = historial.stream().mapToInt(MovimientoSaldo::getMinutos).sum();
        assertThat(suma).isEqualTo(saldo(funcionario));
        assertThat(historial.get(0).getSaldoResultanteMinutos()).isEqualTo(saldo(funcionario));
    }

    // ---------------------------------------------------------------------

    private Usuario brenda() {
        return usuarioRepositorio.findByUsername("brenda").orElseThrow();
    }

    private Empleado crearFuncionario(String nombre, int dias, int horas) {
        FormularioEmpleado formulario = new FormularioEmpleado();
        formulario.setNombreCompleto(nombre);
        formulario.setSaldoDias(dias);
        formulario.setSaldoHoras(horas);
        return servicioEmpleados.crear(formulario, brenda().getId());
    }

    private int saldo(Empleado funcionario) {
        return empleadoRepositorio.findById(funcionario.getId()).orElseThrow().getSaldoVacacionesMinutos();
    }

    private String saldoTexto(Empleado funcionario) {
        return ConversorTiempo.formatear(saldo(funcionario), JORNADA);
    }

    private static FormularioSolicitud vacaciones(Empleado funcionario, LocalDate inicio, LocalDate fin,
                                                  int dias, int horasSueltas) {
        FormularioSolicitud formulario = new FormularioSolicitud();
        formulario.setEmpleadoId(funcionario.getId());
        formulario.setTipo(TipoSolicitud.VACACIONES);
        formulario.setFechaInicio(inicio);
        formulario.setFechaFin(fin);
        formulario.setDias(dias);
        formulario.setHorasSueltas(horasSueltas);
        return formulario;
    }

    private static FormularioSolicitud conHoras(Empleado funcionario, TipoSolicitud tipo, LocalDate dia,
                                                int horas, int minutos) {
        FormularioSolicitud formulario = new FormularioSolicitud();
        formulario.setEmpleadoId(funcionario.getId());
        formulario.setTipo(tipo);
        formulario.setFechaInicio(dia);
        formulario.setFechaFin(dia);
        formulario.setHoras(horas);
        formulario.setMinutos(minutos);
        return formulario;
    }
}
