package ni.gestionvacaciones.solicitud;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla {@code solicitud}. Nunca se usa {@code delete}. */
public interface SolicitudRepositorio extends JpaRepository<Solicitud, Long> {

    /** Historial de un funcionario, de la fecha más reciente a la más antigua. */
    List<Solicitud> findByEmpleadoIdOrderByFechaInicioDescIdDesc(Long empleadoId);

    /**
     * Solicitudes del mismo funcionario, en el estado indicado, que se cruzan con
     * el rango de fechas. Dos rangos se cruzan si cada uno empieza antes de que
     * termine el otro.
     */
    @Query("""
            select s from Solicitud s
            where s.empleadoId = :empleadoId
              and s.estado = :estado
              and s.fechaInicio <= :fin
              and s.fechaFin >= :inicio
            order by s.fechaInicio
            """)
    List<Solicitud> buscarTraslapes(@Param("empleadoId") Long empleadoId,
                                    @Param("estado") EstadoSolicitud estado,
                                    @Param("inicio") LocalDate inicio,
                                    @Param("fin") LocalDate fin);

    /**
     * Minutos por tipo, en un rango de fechas de inicio. Para el desglose del año
     * en la ficha: cuánto se fue en vacaciones, en citas y en permisos.
     */
    @Query("""
            select s.tipo, sum(s.minutosSolicitados) from Solicitud s
            where s.empleadoId = :empleadoId
              and s.estado = :estado
              and s.fechaInicio between :desde and :hasta
            group by s.tipo
            """)
    List<Object[]> sumarMinutosPorTipo(@Param("empleadoId") Long empleadoId,
                                       @Param("estado") EstadoSolicitud estado,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta);

    /**
     * Lee la solicitud BLOQUEANDO su fila hasta el fin de la transacción.
     * Así dos anulaciones al mismo tiempo no pueden devolver el tiempo dos
     * veces: la segunda espera y encuentra la solicitud ya anulada.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Solicitud s where s.id = :id")
    Optional<Solicitud> bloquearParaAnular(@Param("id") Long id);
}
