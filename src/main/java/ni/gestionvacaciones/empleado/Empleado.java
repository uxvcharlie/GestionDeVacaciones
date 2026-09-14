package ni.gestionvacaciones.empleado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Un trabajador del que se lleva el control de vacaciones, citas y permisos.
 *
 * <p>No es un usuario del sistema: no entra a la aplicación.</p>
 *
 * <p>Nunca se borra. Darlo de baja pone {@code activo = false} y su historial
 * queda intacto.</p>
 */
@Entity
@Table(name = "empleado")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "cargo", length = 100)
    private String cargo;

    @Column(name = "area_o_dependencia", length = 100)
    private String areaODependencia;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    /**
     * Saldo único en minutos. De acá descuentan los tres tipos de solicitud.
     *
     * <p><b>Regla:</b> este valor solo lo modifica {@code ServicioSaldo}, que
     * siempre deja el movimiento correspondiente en {@code movimiento_saldo}.
     * Ningún otro código debe llamar a {@link #setSaldoVacacionesMinutos}.</p>
     */
    @Column(name = "saldo_vacaciones_minutos", nullable = false)
    private int saldoVacacionesMinutos = 0;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    /** JPA llama a este método justo antes de guardar un cambio. */
    @PreUpdate
    void alActualizar() {
        actualizadoEn = Instant.now();
    }

    /** "Cargo · Área", omitiendo lo que no esté cargado. Para listados y la ficha. */
    public String descripcionPuesto() {
        return Stream.of(cargo, areaODependencia)
                .filter(texto -> texto != null && !texto.isBlank())
                .collect(Collectors.joining(" · "));
    }

    public Long getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getAreaODependencia() {
        return areaODependencia;
    }

    public void setAreaODependencia(String areaODependencia) {
        this.areaODependencia = areaODependencia;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public int getSaldoVacacionesMinutos() {
        return saldoVacacionesMinutos;
    }

    /** Solo para {@code ServicioSaldo}. Ver la regla en el campo. */
    public void setSaldoVacacionesMinutos(int saldoVacacionesMinutos) {
        this.saldoVacacionesMinutos = saldoVacacionesMinutos;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
