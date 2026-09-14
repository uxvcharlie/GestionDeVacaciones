package ni.gestionvacaciones.saldo;

import ni.gestionvacaciones.empleado.Empleado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * El ÚNICO lugar del sistema que modifica el saldo de un empleado.
 *
 * <p>Cada cambio escribe, en la misma transacción, el nuevo saldo del empleado
 * y su renglón en {@code movimiento_saldo}. O se guardan los dos, o ninguno:
 * nunca puede quedar un saldo cambiado sin su rastro.</p>
 *
 * <p>En la Fase 2 solo existe el saldo inicial. El consumo, la reversión y el
 * ajuste manual se agregan acá mismo en la Fase 3.</p>
 */
@Service
public class ServicioSaldo {

    private final MovimientoSaldoRepositorio movimientos;

    public ServicioSaldo(MovimientoSaldoRepositorio movimientos) {
        this.movimientos = movimientos;
    }

    /**
     * Carga el saldo con el que se registra a un empleado nuevo.
     *
     * <p>MANDATORY: exige que quien llama ya tenga una transacción abierta (la
     * de crear el empleado). Así el empleado y su saldo inicial se guardan
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

    @Transactional(readOnly = true)
    public List<MovimientoSaldo> historial(Long empleadoId) {
        return movimientos.findByEmpleadoIdOrderByCreadoEnDescIdDesc(empleadoId);
    }
}
