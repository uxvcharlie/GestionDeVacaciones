package ni.gestionvacaciones.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a la bitácora. Solo se inserta y se lee. */
public interface AuditoriaRepositorio extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByEntidadAndEntidadIdOrderByCreadoEnDescIdDesc(String entidad, Long entidadId);
}
