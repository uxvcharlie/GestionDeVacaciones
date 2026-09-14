package ni.gestionvacaciones.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** La pantalla de parámetros. {@code confirmado} viene de la pantalla de confirmación. */
public class FormularioParametros {

    @NotNull(message = "Escribí las horas por jornada.")
    @Min(value = 1, message = "La jornada tiene que ser de al menos 1 hora.")
    @Max(value = 24, message = "La jornada no puede tener más de 24 horas.")
    private Integer horasPorJornada;

    @NotBlank(message = "Escribí la zona horaria. Para Nicaragua: America/Managua.")
    private String zonaHoraria;

    private boolean confirmado;

    public Integer getHorasPorJornada() {
        return horasPorJornada;
    }

    public void setHorasPorJornada(Integer horasPorJornada) {
        this.horasPorJornada = horasPorJornada;
    }

    public String getZonaHoraria() {
        return zonaHoraria;
    }

    public void setZonaHoraria(String zonaHoraria) {
        this.zonaHoraria = zonaHoraria;
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public void setConfirmado(boolean confirmado) {
        this.confirmado = confirmado;
    }
}
