package ni.gestionvacaciones.solicitud;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ni.gestionvacaciones.comun.ConversorTiempo;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Los datos de una solicitud mientras se completa y se confirma.
 *
 * <p>La cantidad tiene campos distintos según el tipo, porque se piensa
 * distinto: las vacaciones en días (con horas sueltas para el medio día), y
 * las citas y los permisos en horas y minutos. Son campos separados a
 * propósito: si compartieran nombre, sin JavaScript llegarían dos valores
 * mezclados. El servidor usa solo los que corresponden al tipo elegido.</p>
 */
public class FormularioSolicitud {

    @NotNull(message = "Elegí a qué funcionario le vas a registrar la solicitud.")
    private Long empleadoId;

    @NotNull(message = "Elegí el tipo: vacaciones, cita médica o permiso.")
    private TipoSolicitud tipo;

    @NotNull(message = "Escribí la fecha de inicio.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaInicio;

    @NotNull(message = "Escribí la fecha final. Si es un solo día, poné la misma fecha.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaFin;

    // ---- Vacaciones: días y horas sueltas ----------------------------------

    @NotNull(message = "Escribí los días. Si no hay, poné 0.")
    @Min(value = 0, message = "Los días no pueden ser negativos.")
    @Max(value = 366, message = "Revisá los días: más de 366 en una sola solicitud parece un error de tipeo.")
    private Integer dias = 0;

    @NotNull(message = "Escribí las horas sueltas. Si no hay, poné 0.")
    @Min(value = 0, message = "Las horas no pueden ser negativas.")
    @Max(value = 23, message = "Las horas sueltas van de 0 a 23.")
    private Integer horasSueltas = 0;

    // ---- Citas médicas y permisos: horas y minutos -------------------------

    @NotNull(message = "Escribí las horas. Si no hay, poné 0.")
    @Min(value = 0, message = "Las horas no pueden ser negativas.")
    @Max(value = 23, message = "Las horas van de 0 a 23.")
    private Integer horas = 0;

    @NotNull(message = "Escribí los minutos. Si no hay, poné 0.")
    @Min(value = 0, message = "Los minutos no pueden ser negativos.")
    @Max(value = 59, message = "Los minutos van de 0 a 59.")
    private Integer minutos = 0;

    @Size(max = 300, message = "El motivo puede tener hasta 300 caracteres.")
    private String motivo;

    // ---- Excepción de saldo negativo (solo si el saldo no alcanza) --------

    private boolean autorizarSaldoNegativo;

    @Size(max = 250, message = "El motivo de la autorización puede tener hasta 250 caracteres.")
    private String motivoAutorizacion;

    /** Minutos totales, leyendo solo los campos que corresponden al tipo. */
    public int minutosTotales(int horasPorJornada) {
        if (tipo == null) {
            return 0;
        }
        return tipo.isSeCuentaEnDias()
                ? ConversorTiempo.aMinutos(dias, horasSueltas, 0, horasPorJornada)
                : ConversorTiempo.aMinutos(0, horas, minutos, horasPorJornada);
    }

    public Long getEmpleadoId() {
        return empleadoId;
    }

    public void setEmpleadoId(Long empleadoId) {
        this.empleadoId = empleadoId;
    }

    public TipoSolicitud getTipo() {
        return tipo;
    }

    public void setTipo(TipoSolicitud tipo) {
        this.tipo = tipo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Integer getDias() {
        return dias;
    }

    public void setDias(Integer dias) {
        this.dias = dias;
    }

    public Integer getHorasSueltas() {
        return horasSueltas;
    }

    public void setHorasSueltas(Integer horasSueltas) {
        this.horasSueltas = horasSueltas;
    }

    public Integer getHoras() {
        return horas;
    }

    public void setHoras(Integer horas) {
        this.horas = horas;
    }

    public Integer getMinutos() {
        return minutos;
    }

    public void setMinutos(Integer minutos) {
        this.minutos = minutos;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public boolean isAutorizarSaldoNegativo() {
        return autorizarSaldoNegativo;
    }

    public void setAutorizarSaldoNegativo(boolean autorizarSaldoNegativo) {
        this.autorizarSaldoNegativo = autorizarSaldoNegativo;
    }

    public String getMotivoAutorizacion() {
        return motivoAutorizacion;
    }

    public void setMotivoAutorizacion(String motivoAutorizacion) {
        this.motivoAutorizacion = motivoAutorizacion;
    }
}
