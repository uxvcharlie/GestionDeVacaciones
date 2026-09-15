package ni.gestionvacaciones.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

/** Acceso a la bitácora. Solo se inserta y se lee. */
public interface AuditoriaRepositorio extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByEntidadAndEntidadIdOrderByCreadoEnDescIdDesc(String entidad, Long entidadId);

    /** Para la pantalla de bitácora, sin filtro de acción. */
    Page<Auditoria> findByCreadoEnGreaterThanEqualAndCreadoEnLessThan(Instant desde, Instant hasta, Pageable pagina);

    /** Para la pantalla de bitácora, filtrando por una acción. */
    Page<Auditoria> findByAccionAndCreadoEnGreaterThanEqualAndCreadoEnLessThan(String accion, Instant desde,
                                                                               Instant hasta, Pageable pagina);
}
