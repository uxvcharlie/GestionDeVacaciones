package ni.gestionvacaciones.saldo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ni.gestionvacaciones.comun.ConversorTiempo;

/**
 * Formulario del ajuste manual de saldo: sumar o restar tiempo, siempre con
 * motivo. El motivo queda en el historial del saldo y en la bitácora.
 */
public class FormularioAjuste {

    public enum Operacion {
        SUMAR, RESTAR
    }

    @NotNull(message = "Elegí si vas a sumar o a restar.")
    private Operacion operacion;

    @NotNull(message = "Escribí los días. Si no hay, poné 0.")
    @Min(value = 0, message = "Los días no pueden ser negativos.")
    @Max(value = 3650, message = "Revisá los días: más de 3650 parece un error de tipeo.")
    private Integer dias = 0;

    @NotNull(message = "Escribí las horas. Si no hay, poné 0.")
    @Min(value = 0, message = "Las horas no pueden ser negativas.")
    @Max(value = 23, message = "Las horas van de 0 a 23.")
    private Integer horas = 0;

    @NotNull(message = "Escribí los minutos. Si no hay, poné 0.")
    @Min(value = 0, message = "Los minutos no pueden ser negativos.")
    @Max(value = 59, message = "Los minutos van de 0 a 59.")
    private Integer minutos = 0;

    @NotBlank(message = "Escribí el motivo del ajuste. Queda guardado en el historial.")
    @Size(max = 300, message = "El motivo puede tener hasta 300 caracteres.")
    private String motivo;

    private boolean autorizarSaldoNegativo;

    /** Positivo si suma, negativo si resta. */
    public int minutosConSigno(int horasPorJornada) {
        int total = ConversorTiempo.aMinutos(dias, horas, minutos, horasPorJornada);
        return operacion == Operacion.RESTAR ? -total : total;
    }

    public Operacion getOperacion() {
        return operacion;
    }

    public void setOperacion(Operacion operacion) {
        this.operacion = operacion;
    }

    public Integer getDias() {
        return dias;
    }

    public void setDias(Integer dias) {
        this.dias = dias;
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
}
