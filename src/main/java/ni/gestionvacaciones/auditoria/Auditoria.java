package ni.gestionvacaciones.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Un renglón de la bitácora: quién hizo qué, sobre qué, cuándo y desde dónde.
 * Como el libro del saldo, nunca se modifica: no tiene setters.
 */
@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;

    @Column(name = "accion", nullable = false, length = 60, updatable = false)
    private String accion;

    @Column(name = "entidad", length = 40, updatable = false)
    private String entidad;

    @Column(name = "entidad_id", updatable = false)
    private Long entidadId;

    @Column(name = "detalle", length = 1000, updatable = false)
    private String detalle;

    @Column(name = "ip", length = 45, updatable = false)
    private String ip;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    /** Para JPA. */
    protected Auditoria() {
    }

    public Auditoria(Long usuarioId, String accion, String entidad, Long entidadId, String detalle, String ip) {
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.detalle = detalle;
        this.ip = ip;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getAccion() {
        return accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getIp() {
        return ip;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
