package ni.gestionvacaciones.solicitud;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ni.gestionvacaciones.comun.ReglaDeNegocioException;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Una vacación, cita médica o permiso anotado en el sistema.
 *
 * <p>Nunca se borra: si hubo un error, se anula con {@link #anular}, que exige
 * un motivo. Los datos de la solicitud no tienen setters: una vez registrada,
 * lo único que puede cambiar es su estado.</p>
 */
@Entity
@Table(name = "solicitud")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empleado_id", nullable = false, updatable = false)
    private Long empleadoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20, updatable = false)
    private TipoSolicitud tipo;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false, updatable = false)
    private LocalDate fechaFin;

    @Column(name = "minutos_solicitados", nullable = false, updatable = false)
    private int minutosSolicitados;

    @Column(name = "motivo", length = 300, updatable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.REGISTRADA;

    @Column(name = "registrado_por", nullable = false, updatable = false)
    private Long registradoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "anulado_en")
    private Instant anuladoEn;

    @Column(name = "anulado_por")
    private Long anuladoPor;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    /** Para JPA. */
    protected Solicitud() {
    }

    public Solicitud(Long empleadoId, TipoSolicitud tipo, LocalDate fechaInicio, LocalDate fechaFin,
                     int minutosSolicitados, String motivo, Long registradoPor) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new ReglaDeNegocioException("La fecha final no puede ser anterior a la fecha de inicio.");
        }
        if (minutosSolicitados <= 0) {
            throw new ReglaDeNegocioException("La cantidad de tiempo tiene que ser mayor que cero.");
        }
        this.empleadoId = empleadoId;
        this.tipo = tipo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.minutosSolicitados = minutosSolicitados;
        this.motivo = motivo;
        this.registradoPor = registradoPor;
    }

    /**
     * Anula la solicitud. Solo cambia el estado: devolver el tiempo al saldo lo
     * hace {@code ServicioSolicitudes}, en la misma transacción.
     */
    public void anular(Long usuarioId, String motivoDeAnulacion) {
        if (estado == EstadoSolicitud.ANULADA) {
            throw new ReglaDeNegocioException("Esta solicitud ya estaba anulada. El tiempo ya se había devuelto.");
        }
        if (motivoDeAnulacion == null || motivoDeAnulacion.isBlank()) {
            throw new ReglaDeNegocioException("Escribí el motivo de la anulación.");
        }
        this.estado = EstadoSolicitud.ANULADA;
        this.anuladoEn = Instant.now();
        this.anuladoPor = usuarioId;
        this.motivoAnulacion = motivoDeAnulacion.trim();
    }

    public boolean isRegistrada() {
        return estado == EstadoSolicitud.REGISTRADA;
    }

    /** ¿Empieza y termina el mismo día? Para escribir "el 21/09" en vez de "del 21/09 al 21/09". */
    public boolean isDeUnSoloDia() {
        return fechaInicio.equals(fechaFin);
    }

    public Long getId() {
        return id;
    }

    public Long getEmpleadoId() {
        return empleadoId;
    }

    public TipoSolicitud getTipo() {
        return tipo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public int getMinutosSolicitados() {
        return minutosSolicitados;
    }

    public String getMotivo() {
        return motivo;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public Long getRegistradoPor() {
        return registradoPor;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getAnuladoEn() {
        return anuladoEn;
    }

    public Long getAnuladoPor() {
        return anuladoPor;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }
}
