package ni.gestionvacaciones.empleado;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a la tabla {@code empleado}. Nunca se usa {@code delete}. */
public interface EmpleadoRepositorio extends JpaRepository<Empleado, Long> {

    List<Empleado> findByActivoTrueOrderByNombreCompletoAsc();

    List<Empleado> findAllByOrderByNombreCompletoAsc();
}
