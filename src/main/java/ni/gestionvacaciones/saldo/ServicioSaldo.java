package ni.gestionvacaciones.saldo;

import ni.gestionvacaciones.auditoria.AccionAuditoria;
import ni.gestionvacaciones.auditoria.ServicioAuditoria;
import ni.gestionvacaciones.comun.ConversorTiempo;
import ni.gestionvacaciones.comun.ReglaDeNegocioException;
import ni.gestionvacaciones.empleado.Empleado;
import ni.gestionvacaciones.empleado.EmpleadoNoEncontradoException;
import ni.gestionvacaciones.empleado.EmpleadoRepositorio;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.seguridad.Rol;
import ni.gestionvacaciones.seguridad.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * El ÚNICO lugar del sistema que modifica el saldo de un funcionario.
 *
 * <p>Cada cambio escribe, en la misma transacción, el nuevo saldo y su renglón
 * en {@code movimiento_saldo}. O se guardan los dos, o ninguno: nunca puede
 * quedar un saldo cambiado sin su rastro.</p>
 *
 * <p>Antes de cambiar el saldo se BLOQUEA la fila del funcionario. Si dos
 * registros llegan al mismo tiempo, el segundo espera y trabaja con el saldo
 * ya descontado por el primero.</p>
 */
@Service
public class ServicioSaldo {

    private static final int LARGO_DESCRIPCION = 300;

    private final MovimientoSaldoRepositorio movimientos;
    private final EmpleadoRepositorio empleados;
    private final ServicioParametros parametros;
    private final ServicioAuditoria auditoria;

    public ServicioSaldo(MovimientoSaldoRepositorio movimientos,
                         EmpleadoRepositorio empleados,
                         ServicioParametros parametros,
                         ServicioAuditoria auditoria) {
        this.movimientos = movimientos;
        this.empleados = empleados;
        this.parametros = parametros;
        this.auditoria = auditoria;
    }

    /**
     * Carga el saldo con el que se registra a un funcionario nuevo.
     *
     * <p>MANDATORY: exige que quien llama ya tenga una transacción abierta (la
     * de crear el funcionario). Así el funcionario y su saldo inicial se guardan
     * juntos o no se guarda nada.</p>
     *
     * <p>Si el saldo inicial es cero no se escribe movimiento: no hubo ningún
     * cambio que registrar, y la base de datos no admite movimientos de cero.</p>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarSaldoInicial(Empleado empleado, int minutos, Long realizadoPor) {
        if (empleado.getId() == null) {
            throw new IllegalStateException("El empleado tiene que estar guardado antes de cargarle saldo.");
        }
        if (minutos < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo.");
        }
        empleado.setSaldoVacacionesMinutos(minutos);
        if (minutos == 0) {
            return;
        }
        movimientos.save(new MovimientoSaldo(
                empleado.getId(), null, TipoMovimiento.SALDO_INICIAL,
                minutos, minutos, "Saldo con el que se registró al funcionario", realizadoPor));
    }

    /**
     * Suma o resta minutos al saldo y deja su movimiento.
     *
     * <p>Es la red de seguridad final: aunque quien llama ya haya revisado el
     * saldo, acá se vuelve a revisar con la fila bloqueada. Una resta que deja
     * el saldo en negativo se rechaza salvo que venga autorizada.</p>
     *
     * @param minutos           positivo suma, negativo resta; nunca cero
     * @param permitirNegativo  true solo con autorización expresa de un ADMIN
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public CambioDeSaldo aplicar(Long empleadoId, TipoMovimiento tipo, int minutos, Long solicitudId,
                                 String descripcion, Long realizadoPor, boolean permitirNegativo) {
        if (tipo == TipoMovimiento.SALDO_INICIAL) {
            throw new IllegalArgumentException("El saldo inicial se carga con registrarSaldoInicial.");
        }
        Empleado empleado = empleados.bloquearParaCambiarSaldo(empleadoId)
                .orElseThrow(() -> new EmpleadoNoEncontradoException(empleadoId));

        int antes = empleado.getSaldoVacacionesMinutos();
        int despues = Math.addExact(antes, minutos);
        if (minutos < 0 && despues < 0 && !permitirNegativo) {
            throw new ReglaDeNegocioException("El saldo no alcanza para este movimiento.", true);
        }

        empleado.setSaldoVacacionesMinutos(despues);
        movimientos.save(new MovimientoSaldo(
                empleadoId, solicitudId, tipo, minutos, despues, recortar(descripcion), realizadoPor));
        return new CambioDeSaldo(empleado, antes, despues);
    }

    /**
     * Ajuste manual: sumar o restar tiempo, siempre con motivo.
     *
     * <p>Si la resta deja el saldo en negativo, se pide la misma autorización
     * expresa que en las solicitudes, y queda en la bitácora.</p>
     */
    @Transactional
    public CambioDeSaldo ajustarManual(Long empleadoId, FormularioAjuste formulario, Usuario usuario, String ip) {
        String motivo = formulario.getMotivo() == null ? "" : formulario.getMotivo().trim();
        if (motivo.isEmpty()) {
            throw new ReglaDeNegocioException("Escribí el motivo del ajuste. Queda guardado en el historial.");
        }
        if (formulario.getOperacion() == null) {
            throw new ReglaDeNegocioException("Elegí si vas a sumar o a restar.");
        }
        int jornada = parametros.horasPorJornada();
        int minutos = formulario.minutosConSigno(jornada);
        if (minutos == 0) {
            throw new ReglaDeNegocioException("Escribí cuánto vas a sumar o restar. No puede ser cero.");
        }

        Empleado empleado = empleados.bloquearParaCambiarSaldo(empleadoId)
                .orElseThrow(() -> new EmpleadoNoEncontradoException(empleadoId));
        if (!empleado.isActivo()) {
            throw new ReglaDeNegocioException(
                    empleado.getNombreCompleto() + " está de baja: no se le puede ajustar el saldo.");
        }

        int antes = empleado.getSaldoVacacionesMinutos();
        boolean quedaNegativo = minutos < 0 && antes + (long) minutos < 0;
        if (quedaNegativo) {
            if (usuario.getRol() != Rol.ADMIN) {
                throw new ReglaDeNegocioException("Solo una persona administradora puede dejar un saldo en negativo.");
            }
            if (!formulario.isAutorizarSaldoNegativo()) {
                throw new ReglaDeNegocioException(usuario.primerNombre() + ", " + empleado.getNombreCompleto()
                        + " solo tiene " + ConversorTiempo.formatear(antes, jornada) + " de saldo y estás restando "
                        + ConversorTiempo.formatear(-minutos, jornada)
                        + ". Si está autorizado que quede en negativo, marcá la casilla de autorización.", true);
            }
        }

        CambioDeSaldo cambio = aplicar(empleadoId, TipoMovimiento.AJUSTE_MANUAL, minutos, null,
                motivo, usuario.getId(), quedaNegativo);

        auditoria.registrar(usuario.getId(), AccionAuditoria.SALDO_AJUSTADO,
                ServicioAuditoria.ENTIDAD_EMPLEADO, empleadoId,
                empleado.getNombreCompleto() + " · " + (minutos > 0 ? "Se sumaron " : "Se restaron ")
                        + ConversorTiempo.formatear(Math.abs(minutos), jornada)
                        + " · Saldo: " + ConversorTiempo.formatear(cambio.saldoAntes(), jornada)
                        + " → " + ConversorTiempo.formatear(cambio.saldoDespues(), jornada)
                        + " · Motivo: " + motivo,
                ip);
        if (quedaNegativo) {
            auditoria.registrar(usuario.getId(), AccionAuditoria.SALDO_NEGATIVO_AUTORIZADO,
                    ServicioAuditoria.ENTIDAD_EMPLEADO, empleadoId,
                    empleado.getNombreCompleto() + " quedó con un saldo de "
                            + ConversorTiempo.formatear(cambio.saldoDespues(), jornada)
                            + " por un ajuste manual. Motivo: " + motivo,
                    ip);
        }
        return cambio;
    }

    @Transactional(readOnly = true)
    public List<MovimientoSaldo> historial(Long empleadoId) {
        return movimientos.findByEmpleadoIdOrderByCreadoEnDescIdDesc(empleadoId);
    }

    private static String recortar(String texto) {
        if (texto == null || texto.length() <= LARGO_DESCRIPCION) {
            return texto;
        }
        return texto.substring(0, LARGO_DESCRIPCION - 1) + "…";
    }
}
