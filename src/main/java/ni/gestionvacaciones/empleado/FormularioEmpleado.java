package ni.gestionvacaciones.empleado;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Los campos del formulario para agregar o editar un empleado.
 *
 * <p>Estas validaciones corren en el SERVIDOR. Las del navegador (required,
 * maxlength) son solo comodidad: cualquiera puede saltárselas.</p>
 *
 * <p>El saldo se escribe en días y horas enteros, nunca con decimales. Solo se
 * usa al agregar: al editar se ignora, porque el saldo únicamente cambia con
 * solicitudes o con un ajuste manual (Fase 3).</p>
 */
public class FormularioEmpleado {

    @NotBlank(message = "Escribí el nombre completo.")
    @Size(max = 150, message = "El nombre puede tener hasta 150 caracteres.")
    private String nombreCompleto;

    @Size(max = 100, message = "El cargo puede tener hasta 100 caracteres.")
    private String cargo;

    @Size(max = 100, message = "El área o dependencia puede tener hasta 100 caracteres.")
    private String areaODependencia;

    @PastOrPresent(message = "La fecha de ingreso no puede estar en el futuro.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaIngreso;

    @NotNull(message = "Escribí los días de saldo. Si no tiene, poné 0.")
    @Min(value = 0, message = "Los días no pueden ser negativos.")
    @Max(value = 3650, message = "Revisá los días: más de 3650 parece un error de tipeo.")
    private Integer saldoDias = 0;

    @NotNull(message = "Escribí las horas de saldo. Si no tiene, poné 0.")
    @Min(value = 0, message = "Las horas no pueden ser negativas.")
    @Max(value = 23, message = "Las horas van de 0 a 23. Si son más, sumalas como días.")
    private Integer saldoHoras = 0;

    /** Precarga el formulario de edición con los datos actuales. */
    public static FormularioEmpleado desde(Empleado empleado) {
        FormularioEmpleado formulario = new FormularioEmpleado();
        formulario.nombreCompleto = empleado.getNombreCompleto();
        formulario.cargo = empleado.getCargo();
        formulario.areaODependencia = empleado.getAreaODependencia();
        formulario.fechaIngreso = empleado.getFechaIngreso();
        return formulario;
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

    public Integer getSaldoDias() {
        return saldoDias;
    }

    public void setSaldoDias(Integer saldoDias) {
        this.saldoDias = saldoDias;
    }

    public Integer getSaldoHoras() {
        return saldoHoras;
    }

    public void setSaldoHoras(Integer saldoHoras) {
        this.saldoHoras = saldoHoras;
    }
}
