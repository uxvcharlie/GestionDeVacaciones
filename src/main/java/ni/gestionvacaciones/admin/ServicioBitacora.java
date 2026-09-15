package ni.gestionvacaciones.admin;

import ni.gestionvacaciones.auditoria.Auditoria;
import ni.gestionvacaciones.auditoria.AuditoriaRepositorio;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Consulta de la bitácora, de a 50 renglones, del más reciente al más antiguo. */
@Service
public class ServicioBitacora {

    public static final int RENGLONES_POR_PAGINA = 50;

    /** Sin fecha "hasta", se busca hasta muy lejos en el futuro. */
    private static final Instant SIN_LIMITE = Instant.parse("9999-12-31T00:00:00Z");

    private final AuditoriaRepositorio repositorio;
    private final ServicioUsuario servicioUsuario;
    private final ServicioParametros parametros;

    public ServicioBitacora(AuditoriaRepositorio repositorio,
                            ServicioUsuario servicioUsuario,
                            ServicioParametros parametros) {
        this.repositorio = repositorio;
        this.servicioUsuario = servicioUsuario;
        this.parametros = parametros;
    }

    /**
     * @param accion  null para todas
     * @param desde   primer día incluido, en hora de Managua; null sin límite
     * @param hasta   último día incluido, en hora de Managua; null sin límite
     */
    @Transactional(readOnly = true)
    public PaginaBitacora buscar(String accion, LocalDate desde, LocalDate hasta, int pagina) {
        ZoneId zona = parametros.zonaHoraria();
        Instant inicio = desde == null ? Instant.EPOCH : desde.atStartOfDay(zona).toInstant();
        Instant fin = hasta == null ? SIN_LIMITE : hasta.plusDays(1).atStartOfDay(zona).toInstant();
        Pageable orden = PageRequest.of(Math.max(pagina, 0), RENGLONES_POR_PAGINA,
                Sort.by(Sort.Order.desc("creadoEn"), Sort.Order.desc("id")));

        Page<Auditoria> resultado = accion == null
                ? repositorio.findByCreadoEnGreaterThanEqualAndCreadoEnLessThan(inicio, fin, orden)
                : repositorio.findByAccionAndCreadoEnGreaterThanEqualAndCreadoEnLessThan(accion, inicio, fin, orden);

        Set<Long> usuarios = resultado.getContent().stream()
                .map(Auditoria::getUsuarioId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return new PaginaBitacora(resultado.getContent(), servicioUsuario.nombresPorId(usuarios),
                resultado.getNumber(), resultado.hasPrevious(), resultado.hasNext(), resultado.getTotalElements());
    }
}
