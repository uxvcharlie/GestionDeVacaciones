package ni.gestionvacaciones.empleado;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Acceso a la tabla {@code empleado}. Nunca se usa {@code delete}. */
public interface EmpleadoRepositorio extends JpaRepository<Empleado, Long> {

    List<Empleado> findByActivoTrueOrderByNombreCompletoAsc();

    List<Empleado> findAllByOrderByNombreCompletoAsc();

    /**
     * Lee el empleado y BLOQUEA su fila hasta que termine la transacción
     * ({@code SELECT ... FOR UPDATE}).
     *
     * <p>Evita el error clásico de dos registros al mismo tiempo: los dos leen
     * "10 días", cada uno resta 8, y el saldo queda en 2 en vez de rechazar el
     * segundo. Con el bloqueo, el segundo espera a que termine el primero y lee
     * el saldo ya descontado.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Empleado e where e.id = :id")
    Optional<Empleado> bloquearParaCambiarSaldo(@Param("id") Long id);
}
