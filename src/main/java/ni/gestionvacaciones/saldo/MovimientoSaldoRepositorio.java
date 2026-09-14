package ni.gestionvacaciones.saldo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a {@code movimiento_saldo}. Solo se inserta y se lee; nunca se borra. */
public interface MovimientoSaldoRepositorio extends JpaRepository<MovimientoSaldo, Long> {

    /** Historial de un empleado, del más reciente al más antiguo. */
    List<MovimientoSaldo> findByEmpleadoIdOrderByCreadoEnDescIdDesc(Long empleadoId);
}
