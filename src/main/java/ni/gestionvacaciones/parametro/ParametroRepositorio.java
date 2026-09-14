package ni.gestionvacaciones.parametro;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla {@code parametro}. La clave es el identificador. */
public interface ParametroRepositorio extends JpaRepository<Parametro, String> {
}
