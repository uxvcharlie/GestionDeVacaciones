package ni.gestionvacaciones.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El motivo es obligatorio: una anulación sin explicación no se acepta. */
public class FormularioAnulacion {

    @NotBlank(message = "Escribí el motivo de la anulación.")
    @Size(max = 300, message = "El motivo puede tener hasta 300 caracteres.")
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
