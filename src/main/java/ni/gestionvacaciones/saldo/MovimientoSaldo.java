package ni.gestionvacaciones.saldo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Un renglón del libro contable del saldo de un empleado.
 *
 * <p>Los movimientos nunca se modifican ni se borran: si algo estuvo mal, se
 * corrige con un movimiento nuevo. Por eso esta clase no tiene setters
 * públicos; se construye completa de una vez.</p>
 */
@Entity
@Table(name = "movimiento_saldo")
public class MovimientoSaldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empleado_id", nullable = false, updatable = false)
    private Long empleadoId;

    @Column(name = "solicitud_id", updatable = false)
    private Long solicitudId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 20, updatable = false)
    private TipoMovimiento tipoMovimiento;

    /** Positivo suma, negativo resta. Nunca cero. */
    @Column(name = "minutos", nullable = false, updatable = false)
    private int minutos;

    @Column(name = "saldo_resultante_minutos", nullable = false, updatable = false)
    private int saldoResultanteMinutos;

    @Column(name = "descripcion", length = 300, updatable = false)
    private String descripcion;

    @Column(name = "realizado_por", nullable = false, updatable = false)
    private Long realizadoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    /** Para JPA. */
    protected MovimientoSaldo() {
    }

    public MovimientoSaldo(Long empleadoId, Long solicitudId, TipoMovimiento tipoMovimiento,
                           int minutos, int saldoResultanteMinutos, String descripcion, Long realizadoPor) {
        if (minutos == 0) {
            throw new IllegalArgumentException("Un movimiento de saldo no puede ser de cero minutos.");
        }
        this.empleadoId = empleadoId;
        this.solicitudId = solicitudId;
        this.tipoMovimiento = tipoMovimiento;
        this.minutos = minutos;
        this.saldoResultanteMinutos = saldoResultanteMinutos;
        this.descripcion = descripcion;
        this.realizadoPor = realizadoPor;
    }

    public Long getId() {
        return id;
    }

    public Long getEmpleadoId() {
        return empleadoId;
    }

    public Long getSolicitudId() {
        return solicitudId;
    }

    public TipoMovimiento getTipoMovimiento() {
        return tipoMovimiento;
    }

    public int getMinutos() {
        return minutos;
    }

    public int getSaldoResultanteMinutos() {
        return saldoResultanteMinutos;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Long getRealizadoPor() {
        return realizadoPor;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
