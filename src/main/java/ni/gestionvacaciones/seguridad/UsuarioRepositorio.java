package ni.gestionvacaciones.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code usuario}.
 *
 * <p>No escribimos SQL: Spring Data JPA genera la consulta a partir del nombre
 * del método. {@code buscarPorUsername} no seguiría esa convención, por eso el
 * método se llama {@code findByUsername}: es el único lugar donde usamos
 * inglés, porque es la palabra clave que lee Spring.</p>
 */
public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Usuario> findAllByOrderByNombreCompletoAsc();

    long countByActivoTrueAndRol(Rol rol);
}
